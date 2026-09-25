package com.fatec.muttley.qrcode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fatec.muttley.evento.EventoService;
import com.fatec.muttley.qrcode.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.fatec.muttley.evento.enums.StatusEventoEnum.*;
import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QrCodeMensageriaTest {
    @ParameterizedTest @EnumSource(TipoQrCode.class)
    void publicaSomenteDepoisDoCommit(TipoQrCode tipo) {
        KafkaTemplate<String,QrCodeRequest> kafka=mock(KafkaTemplate.class);
        TransactionSynchronizationManager.initSynchronization();
        try {
            new QrCodeProducer(kafka).publicar(evento(CRIADO),"https://example.invalid",tipo);
            verifyNoInteractions(kafka);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(kafka).send(eq("qrcode.gerar.request"),eq("10-"+tipo),any(QrCodeRequest.class));
        } finally {TransactionSynchronizationManager.clearSynchronization();}
    }
    @Test void rollbackNaoPublicaQrCodeParaEventoInexistente() {
        KafkaTemplate<String,QrCodeRequest> kafka=mock(KafkaTemplate.class);
        TransactionSynchronizationManager.initSynchronization();
        try {
            new QrCodeProducer(kafka).publicar(evento(CRIADO),"https://example.invalid",TipoQrCode.INSCRICAO);
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verifyNoInteractions(kafka);
        } finally {TransactionSynchronizationManager.clearSynchronization();}
    }
    @ParameterizedTest @EnumSource(TipoQrCode.class)
    void requestMantemTopicoChaveEDadosDoEvento(TipoQrCode tipo) {
        KafkaTemplate<String,QrCodeRequest> kafka=mock(KafkaTemplate.class);var producer=new QrCodeProducer(kafka);var e=evento(CRIADO);
        if(tipo==TipoQrCode.INSCRICAO)producer.publicarQrCodeInscricao(e,"https://example.invalid");
        else producer.publicarQrCodeConfirmacao(e,"https://example.invalid");
        verify(kafka).send("qrcode.gerar.request","10-"+tipo,new QrCodeRequest(10L,"https://example.invalid",e.getTema(),tipo));
    }
    @ParameterizedTest @EnumSource(TipoQrCode.class)
    void responseAtualizaSomenteUrlCorrespondente(TipoQrCode tipo) throws Exception {
        EventoService eventos=mock(EventoService.class);var consumer=new QrCodeResponseConsumer(eventos);
        consumer.consumir(new ObjectMapper().writeValueAsString(new QrCodeResponse(10L,"https://example.invalid/qr","SUCCESS",null,tipo)));
        if(tipo==TipoQrCode.INSCRICAO)verify(eventos).salvarQrCodeInscricaoUrl(10L,"https://example.invalid/qr");
        else verify(eventos).salvarQrCodeConfirmacaoUrl(10L,"https://example.invalid/qr");
        verifyNoMoreInteractions(eventos);
    }
    @Test void responseErrorNaoModificaEvento() throws Exception {
        EventoService eventos=mock(EventoService.class);
        new QrCodeResponseConsumer(eventos).consumir("{\"eventoId\":10,\"status\":\"ERROR\",\"tipo\":\"INSCRICAO\",\"errorMessage\":\"falha simulada\"}");
        verifyNoInteractions(eventos);
    }
    @Test void jsonInvalidoNaoModificaEvento() {
        EventoService eventos=mock(EventoService.class);
        assertThatThrownBy(() -> new QrCodeResponseConsumer(eventos).consumir("{invalido"))
                .isInstanceOf(JsonProcessingException.class);
        verifyNoInteractions(eventos);
    }
}
