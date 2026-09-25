package com.fatec.muttley.qrcode;

import com.fatec.muttley.qrcode.dto.QrCodeRequest;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class QrCodeClient {

    private final RestClient restClient;

    public QrCodeClient(@Value("${qrcode.ms-url}") String qrCodeMsUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(qrCodeMsUrl)
                .requestFactory(factory)
                .build();
    }

    public byte[] gerarQrCode(QrCodeRequest request) {
        byte[] imagem = restClient.post().uri("/api/qrcode/gerar")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.IMAGE_PNG)
                .body(request).retrieve().body(byte[].class);
        if (imagem == null || imagem.length == 0) {
            throw new IllegalStateException("O serviço de QR Code retornou uma imagem vazia.");
        }
        return imagem;
    }

    public byte[] baixarQrCode(String qrCodeUrl) {
        return restClient.get()
                .uri("/api/qrcode/baixar?url={url}", qrCodeUrl)
                .retrieve()
                .body(byte[].class);
    }
}
