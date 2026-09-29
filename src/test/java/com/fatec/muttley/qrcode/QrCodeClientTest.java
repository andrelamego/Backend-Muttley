package com.fatec.muttley.qrcode;

import com.fatec.muttley.qrcode.dto.QrCodeRequest;
import com.fatec.muttley.qrcode.dto.TipoQrCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeClientTest {
    @Test void enviaContratoAoServicoERecebeBinario() throws Exception {
        var requestRecebido = new AtomicReference<String>();
        var metodo = new AtomicReference<String>();
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47};
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/qrcode/gerar", exchange -> {
            metodo.set(exchange.getRequestMethod());
            requestRecebido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, png.length);
            exchange.getResponseBody().write(png);
            exchange.close();
        });
        servidor.start();
        try {
            var client = new QrCodeClient("http://127.0.0.1:" + servidor.getAddress().getPort());
            var request = new QrCodeRequest(10L, "https://muttley.example.invalid", "Evento", TipoQrCode.CONFIRMACAO);
            assertThat(client.gerarQrCode(request)).containsExactly(png);
            assertThat(metodo.get()).isEqualTo("POST");
            assertThat(new ObjectMapper().readValue(requestRecebido.get(), QrCodeRequest.class)).isEqualTo(request);
        } finally { servidor.stop(0); }
    }

    @Test void respostaIndisponivelNaoEInterpretadaComoImagem() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/qrcode/gerar", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        servidor.start();
        try {
            var client = new QrCodeClient("http://127.0.0.1:" + servidor.getAddress().getPort());
            assertThatThrownBy(() -> client.gerarQrCode(new QrCodeRequest(10L, "https://example.invalid", "Evento", TipoQrCode.INSCRICAO)))
                    .isInstanceOf(RestClientResponseException.class);
        } finally { servidor.stop(0); }
    }
}
