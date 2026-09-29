package com.fatec.muttley.email;

import com.fatec.muttley.email.dto.*;
import com.fatec.muttley.certificado.Certificado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.kafka.core.KafkaTemplate;
import java.time.LocalDate;
import java.util.List;
import static com.fatec.muttley.support.Cenarios.*;
import static com.fatec.muttley.evento.enums.StatusEventoEnum.*;
import static org.mockito.Mockito.*;

class EmailProducerTest {
    @Test void RN_PAR_10_confirmacaoTemDadosEChaveDaInscricao() {
        KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);var p=participacao(42,false);var e=p.getEvento();
        e.setLocal(null);
        new EmailProducer(kafka).publicarConfirmacaoInscricao(p);
        verify(kafka).send("email.inscricao.confirmada","42",new InscricaoEmail(p.getPessoa().getEmail(),p.getPessoa().getNome(),e.getTema(),e.getData().toString(),"09:00","11:30","A definir",42));
    }
    @Test void RN_PAR_11_complementacaoUsaConvite() {
        KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);var p=participacao(1,false);
        var producer=new EmailProducer(kafka);
        producer.publicarCompletarCadastro(p,"https://example.invalid","convite-aleatorio");
        verify(kafka).send("email.completar.cadastro",String.valueOf(p.getPessoa().getId()),new CadastroEmail(p.getPessoa().getEmail(),p.getPessoa().getNome(),"convite-aleatorio","https://example.invalid"));
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void notificacoesDeEventoIncluemPresentesEAusentes(boolean cancelado) {
        KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);var producer=new EmailProducer(kafka);var e=evento(EM_ANDAMENTO);
        var lista=List.of(participacao(1,true),participacao(2,false));
        if(cancelado)producer.publicarEventoCancelado(e,lista);else producer.publicarEventoConcluido(e,lista);
        String topico=cancelado?"email.evento.cancelado":"email.evento.concluido";
        for(var p:lista)verify(kafka).send(topico,"10",new EventoEmail(p.getPessoa().getEmail(),p.getPessoa().getNome(),e.getTema(),e.getData().toString()));
        verifyNoMoreInteractions(kafka);
    }
    @Test void RN_CER_11_enviaApenasListaRecebidaComUrlPublica() {
        KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);var c=new Certificado();var p=participacao(1,true);
        c.setParticipacao(p);c.setDataEmissao(LocalDate.of(2026,9,3));c.setUrlPublica("/certificados/codigo");
        new EmailProducer(kafka).publicarCertificados(List.of(c),"https://example.invalid");
        verify(kafka).send("email.certificado",new CertificadoEmail(p.getPessoa().getEmail(),p.getPessoa().getNome(),p.getEvento().getTema(),p.getEvento().getData().toString(),"2026-09-03","https://example.invalid","/certificados/codigo"));
        verifyNoMoreInteractions(kafka);
    }
    @Test void listasVaziasNaoEnviamMensagens() {
        KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);var producer=new EmailProducer(kafka);
        producer.publicarCertificados(List.of(),"https://example.invalid");producer.publicarEventoCancelado(evento(CRIADO),List.of());producer.publicarEventoConcluido(evento(EM_ANDAMENTO),List.of());
        verifyNoInteractions(kafka);
    }
}
