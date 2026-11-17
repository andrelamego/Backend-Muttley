package com.fatec.muttley.qrcode;

import com.fatec.muttley.evento.Evento;
import com.fatec.muttley.qrcode.dto.QrCodeRequest;
import com.fatec.muttley.qrcode.dto.TipoQrCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeProducer {

    private final KafkaTemplate<String, QrCodeRequest> kafkaTemplate;
    private static final String TOPIC = "qrcode.gerar.request";

    public void publicarQrCodeInscricao(Evento evento, String baseUrl) {
        publicar(evento, baseUrl, TipoQrCode.INSCRICAO);
    }

    public void publicarQrCodeConfirmacao(Evento evento, String baseUrl) {
        publicar(evento, baseUrl, TipoQrCode.CONFIRMACAO);
    }

    public void publicar(Evento evento, String baseUrl, TipoQrCode tipo) {
        var dto = new QrCodeRequest(
                evento.getId(),
                baseUrl,
                evento.getTema(),
                tipo
        );

        String chave = evento.getId() + "-" + tipo.name();
        Runnable envio = () -> {
            kafkaTemplate.send(TOPIC, chave, dto);
            log.info("QR Code enfileirado: eventoId={}, tipo={}", evento.getId(), tipo);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { envio.run(); }
            });
        } else {
            envio.run();
        }
    }
}
