package com.fatec.muttley.certificado;

import com.fatec.muttley.evento.EventoService;
import com.fatec.muttley.evento.enums.StatusEventoEnum;
import com.fatec.muttley.participacao.*;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificadoServiceTest {
    @Mock CertificadoRepository repository;
    @Mock ParticipacaoService participacoes;
    @Spy CertificadoMapper mapper = Mappers.getMapper(CertificadoMapper.class);
    @Spy Clock clock = Clock.systemDefaultZone();
    @InjectMocks CertificadoService service;

    @Test void RF_CER_01_RN_CER_03_04_05_06_emiteComDadosPublicosEAssinatura() {
        Participacao p=participacao(1,true);
        when(participacoes.procurarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(p));
        LocalDate antes=LocalDate.now();
        Certificado c=service.gerarCertificadosParaParticipacoes(List.of(1L),"assinatura.png").getFirst();
        assertThat(c.getDataEmissao()).isBetween(antes,LocalDate.now());
        assertThat(c.getAssinatura()).isEqualTo("Coordenação FATEC");
        assertThat(c.getParticipacao()).isSameAs(p);
        assertThat(UUID.fromString(c.getCodigoValidacao()).toString()).isEqualTo(c.getCodigoValidacao());
        assertThat(c.getUrlPublica()).isEqualTo("/certificados/"+c.getCodigoValidacao());
        assertThat(c.getCaminhoPdf()).isEqualTo(c.getUrlPublica()+".pdf");
        assertThat(c.getCaminhoAssinaturaVisual()).isEqualTo("assinatura.png");
        verify(repository).save(c);
    }
    @Test void RN_CER_01_02_11_ignoraExistentesENulosRetornandoApenasNovos() {
        when(repository.existsByParticipacaoId(1L)).thenReturn(true);
        when(participacoes.procurarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(participacao(1,true)));
        when(participacoes.procurarPorIdParaAtualizacao(2L)).thenReturn(Optional.of(participacao(2,true)));
        List<Certificado> novos=service.gerarCertificadosParaParticipacoes(Arrays.asList(null,1L,2L));
        assertThat(novos).extracting(c -> c.getParticipacao().getId()).containsExactly(2L);
        verify(participacoes).procurarPorIdParaAtualizacao(1L);
        verify(repository,times(1)).save(any());
    }
    @Test void RN_CER_05_tentaOutroUuidEmCasoDeColisao() {
        when(participacoes.procurarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(participacao(1,true)));
        when(repository.existsByCodigoValidacao(anyString())).thenReturn(true,false);
        Certificado c=service.gerarCertificadosParaParticipacoes(List.of(1L)).getFirst();
        ArgumentCaptor<String> ids=ArgumentCaptor.forClass(String.class);
        verify(repository,times(2)).existsByCodigoValidacao(ids.capture());
        assertThat(ids.getAllValues().get(0)).isNotEqualTo(ids.getAllValues().get(1));
        assertThat(c.getCodigoValidacao()).isEqualTo(ids.getValue());
    }
    @Test void participacaoInexistenteNaoEmite() {
        assertThatThrownBy(() -> service.gerarCertificadosParaParticipacoes(List.of(99L)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(repository,never()).save(any());
    }
    @Test void RF_CER_06_atualizaSomenteAssinaturaPreservandoCodigo() {
        Certificado c=new Certificado(); c.setCodigoValidacao("codigo-existente");
        when(repository.findById(5L)).thenReturn(Optional.of(c));
        service.atualizarCaminhoAssinatura(5L,"nova.png");
        assertThat(c.getCodigoValidacao()).isEqualTo("codigo-existente");
        assertThat(c.getCaminhoAssinaturaVisual()).isEqualTo("nova.png"); verify(repository).save(c);
    }
    @Test void RF_CER_07_atualizaTodasAssinaturasDoEventoConsultado() {
        Certificado a=new Certificado(),b=new Certificado();
        when(repository.findByEventoId(10L)).thenReturn(List.of(a,b));
        service.atualizarAssinaturaPorEvento(10L,"nova.png");
        assertThat(List.of(a,b)).extracting(Certificado::getCaminhoAssinaturaVisual).containsOnly("nova.png");
        verify(repository).saveAll(List.of(a,b));
    }
    @Test void RN_CER_06_completaLegadoSemSubstituirCodigoOuLinksExistentes() {
        Certificado c=new Certificado(); c.setCodigoValidacao("codigo"); c.setUrlPublica("/legado/codigo");
        when(repository.findByPessoaIdComDados(1L)).thenReturn(List.of(c));
        assertThat(service.procurarPorPessoa(1L)).containsExactly(c);
        assertThat(c.getCodigoValidacao()).isEqualTo("codigo");
        assertThat(c.getUrlPublica()).isEqualTo("/legado/codigo");
        assertThat(c.getCaminhoPdf()).isEqualTo("/certificados/codigo.pdf"); verify(repository).save(c);
    }
    @Test void consultaCompletaNaoRegravaCertificado() {
        Certificado c=new Certificado(); c.setCodigoValidacao("codigo"); c.setUrlPublica("/codigo"); c.setCaminhoPdf("/codigo.pdf");
        when(repository.findByPessoaIdComDados(1L)).thenReturn(List.of(c)); service.procurarPorPessoa(1L);
        verify(repository,never()).save(any());
    }
    @Test void RF_CER_03_buscaPorCodigoPublico() {
        Certificado c=new Certificado(); when(repository.findByCodigoValidacaoComDados("codigo")).thenReturn(Optional.of(c));
        assertThat(service.procurarPorCodigoValidacao("codigo")).contains(c);
    }
    @Test void RN_CER_01_chamadasRepetidasNaoReemitemCertificado() {
        Set<Long> emitidas=new HashSet<>();
        when(repository.existsByParticipacaoId(anyLong())).thenAnswer(i->emitidas.contains(i.getArgument(0)));
        when(participacoes.procurarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(participacao(1,true)));
        when(repository.save(any())).thenAnswer(i->{Certificado c=i.getArgument(0);emitidas.add(c.getParticipacao().getId());return c;});
        assertThat(service.gerarCertificadosParaParticipacoes(List.of(1L,1L))).hasSize(1);
        assertThat(service.gerarCertificadosParaParticipacoes(List.of(1L))).isEmpty();
        verify(repository,times(1)).save(any());
    }
    @Test void RF_CER_02_listagemAdministrativaAgrupaCertificadosPendenciasERecentes() {
        var certificados=mock(CertificadoService.class);var eventos=mock(EventoService.class);
        var controller=new CertificadoController(certificados,eventos,mock(AssinaturaStorage.class));
        var c=new Certificado();var e=evento(StatusEventoEnum.EM_ANDAMENTO);
        when(certificados.procurarTodos()).thenReturn(List.of(c));when(certificados.procurarUltimosEmitidos()).thenReturn(List.of(c));
        when(eventos.procurarEventosAguardandoEmissaoCertificado()).thenReturn(List.of(e));
        var body=controller.listarDados().getBody();
        assertThat(body.get("certificados")).isEqualTo(List.of(c));assertThat(body.get("ultimosCertificados")).isEqualTo(List.of(c));
        assertThat(body.get("eventosAguardandoCertificado")).isEqualTo(List.of(e));
    }
}
