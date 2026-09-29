package com.fatec.muttley.evento;

import com.fatec.muttley.auth.CadastroConviteService;
import com.fatec.muttley.certificado.*;
import com.fatec.muttley.email.EmailProducer;
import com.fatec.muttley.medalha.MedalhaService;
import com.fatec.muttley.participacao.*;
import com.fatec.muttley.qrcode.*;
import com.fatec.muttley.qrcode.dto.QrCodeRequest;
import com.fatec.muttley.qrcode.dto.TipoQrCode;
import com.fatec.muttley.support.ImagensTeste;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static com.fatec.muttley.evento.enums.StatusEventoEnum.*;
import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoControllerTest {
    @Mock EventoService eventos; @Mock ParticipacaoService participacoes;
    @Mock CertificadoService certificados; @Mock MedalhaService medalhas;
    @Mock EmailProducer emails; @Mock QrCodeProducer qrCodes; @Mock QrCodeClient qrClient;
    @Mock CadastroConviteService convites;
    EventoController controller;
    @TempDir Path diretorio;
    @BeforeEach void configurar() {
        controller = new EventoController(eventos, mock(EventoMapper.class), participacoes, qrCodes, qrClient,
                certificados, medalhas, emails, convites, "https://muttley.example.invalid",
                new AssinaturaStorage(diretorio.toString()), Clock.systemDefaultZone());
    }
    @Test void RN_EVT_14_criacaoSolicitaOsDoisQrCodes() {
        Evento e=evento(CRIADO); var dto=dadosEvento(null,"09:00","11:00");
        when(eventos.salvarOuAtualizar(dto)).thenReturn(e);
        assertThat(controller.criar(new EventoComParticipacaoDTO(dto,null)).getStatusCode().value()).isEqualTo(201);
        verify(qrCodes).publicarQrCodeInscricao(e,"https://muttley.example.invalid");
        verify(qrCodes).publicarQrCodeConfirmacao(e,"https://muttley.example.invalid");
    }
    @Test void RF_EVT_01_02_consultaPublicaRetornaDadosEInscricoesAbertas() {
        Evento e=evento(CRIADO);when(eventos.procurarPorId(10L)).thenReturn(Optional.of(e));
        when(eventos.procurarDisponiveisParaInscricao()).thenReturn(List.of(e));
        var detalhe=controller.buscarEventoPublico(10L).getBody();
        assertThat(detalhe.id()).isEqualTo(10L);assertThat(detalhe.tema()).isEqualTo(e.getTema());
        assertThat(detalhe.inscricoesEncerradas()).isFalse();
        assertThat(controller.listarEventosPublicos().getBody()).containsExactly(detalhe);
    }
    @Test void RF_EVT_08_consultaParticipacoesComPessoaEventoEPresenca() {
        when(eventos.procurarPorId(10L)).thenReturn(Optional.of(evento(EM_ANDAMENTO)));
        var p=participacao(1,true);when(participacoes.procurarPorEvento(10L)).thenReturn(List.of(p));
        assertThat(controller.dadosConclusao(10L).getBody().get("participacoes"))
                .isEqualTo(List.of(ParticipacaoComEventoResponse.from(p)));
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void RN_PAR_10_11_enviaConfirmacaoEComplementacaoSomenteParaParcial(boolean parcial) {
        Participacao p=participacao(1,false); if(!parcial)p.getPessoa().setSenha("hash");
        var request=new InscricaoPublicaRequest("Nome","cpf","teste@example.invalid");
        when(participacoes.registrarInscricaoPublica(10L,request)).thenReturn(p);
        if (parcial) when(convites.emitir(p.getPessoa())).thenReturn(Optional.of("convite"));
        assertThat(controller.registrarInscricaoPublica(10L,request).getStatusCode().value()).isEqualTo(201);
        verify(emails).publicarConfirmacaoInscricao(p);
        verify(emails,times(parcial?1:0)).publicarCompletarCadastro(p,"https://muttley.example.invalid","convite");
    }
    @Test void RN_EVT_13_cancelamentoNotificaTodosInscritosAposCancelar() {
        Evento e=evento(CRIADO); List<Participacao> inscritos=List.of(participacao(1,true),participacao(2,false));
        when(eventos.procurarPorId(10L)).thenReturn(Optional.of(e)); when(participacoes.procurarPorEvento(10L)).thenReturn(inscritos);
        assertThat(controller.cancelar(10L).getStatusCode().value()).isEqualTo(200);
        InOrder ordem=inOrder(eventos,emails); ordem.verify(eventos).cancelarEvento(10L); ordem.verify(emails).publicarEventoCancelado(e,inscritos);
    }
    @Test void cancelamentoRejeitadoNaoNotifica() {
        when(eventos.procurarPorId(10L)).thenReturn(Optional.of(evento(FINALIZADO)));
        doThrow(new IllegalStateException("Finalizado")).when(eventos).cancelarEvento(10L);
        assertThatThrownBy(() -> controller.cancelar(10L)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(emails);
    }
    @Test void conclusaoIgnoraIdsDeOutroEventoEmiteSoParaPresentesEEnviaSoNovos() throws Exception {
        Evento e=evento(EM_ANDAMENTO);
        Participacao antesPresente=participacao(1,true),marcadoAgora=participacao(2,false),ausente=participacao(3,false);
        List<Participacao> inscritos=List.of(antesPresente,marcadoAgora,ausente);
        Certificado novo=new Certificado(); novo.setParticipacao(marcadoAgora);
        when(eventos.procurarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(e));
        when(participacoes.procurarPorEvento(10L)).thenReturn(inscritos);
        when(participacoes.marcarPresente(2L)).thenAnswer(i->{marcadoAgora.setPresente(true);return marcadoAgora;});
        when(certificados.gerarCertificadosParaParticipacoes(anyList(),anyString())).thenReturn(List.of(novo));
        byte[] assinatura=ImagensTeste.criar("png");
        var file=new MockMultipartFile("file","assinatura.png","image/png",assinatura);
        assertThat(controller.concluirEvento(10L,List.of(2L,999L),file).getStatusCode().value()).isEqualTo(200);
        verify(participacoes).marcarPresente(2L); verify(participacoes,never()).marcarPresente(999L);
        ArgumentCaptor<List<Long>> ids=ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> arquivo=ArgumentCaptor.forClass(String.class);
        verify(certificados).gerarCertificadosParaParticipacoes(ids.capture(),arquivo.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(1L,2L);
        Path salvo=Path.of(arquivo.getValue()); assertThat(salvo.getParent()).isEqualTo(diretorio);
        assertThat(Files.readAllBytes(salvo)).containsExactly(assinatura);
        verify(medalhas).gerarMedalhasBronzePorPresenca(inscritos);
        InOrder ordem=inOrder(certificados,eventos,emails);
        ordem.verify(certificados).gerarCertificadosParaParticipacoes(anyList(),anyString());
        ordem.verify(eventos).concluirEvento(10L);
        ordem.verify(emails).publicarEventoConcluido(e,inscritos);
        ordem.verify(emails).publicarCertificados(List.of(novo),"https://muttley.example.invalid");
    }
    @Test void conclusaoSemNovasPresencasUsaConfirmadasAnteriormente() {
        when(eventos.procurarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(evento(EM_ANDAMENTO)));
        when(participacoes.procurarPorEvento(10L)).thenReturn(List.of(participacao(1,true),participacao(2,false)));
        controller.concluirEvento(10L,null,new MockMultipartFile("file","assinatura.png","image/png",ImagensTeste.criar("png")));
        verify(participacoes,never()).marcarPresente(anyLong());
        verify(certificados).gerarCertificadosParaParticipacoes(eq(List.of(1L)),anyString());
    }
    @Test void conclusaoForaDeAndamentoNaoGeraArquivosPremiosOuNotificacoes() throws Exception {
        when(eventos.procurarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(evento(CRIADO)));
        assertThatThrownBy(() -> controller.concluirEvento(10L,List.of(1L),new MockMultipartFile("file",new byte[]{1})))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(certificados,medalhas,participacoes,emails);
        try(var arquivos=Files.list(diretorio)) { assertThat(arquivos).isEmpty(); }
    }
    @Test void RF_PAR_04_RF_MED_03_presencaGeraBronzeDepoisDaConfirmacao() {
        Participacao p=participacao(1,true); when(participacoes.confirmarPresenca(10L,"cpf")).thenReturn(p);
        assertThat(controller.confirmarPresenca(10L,"cpf").getStatusCode().value()).isEqualTo(200);
        InOrder ordem=inOrder(participacoes,medalhas); ordem.verify(participacoes).confirmarPresenca(10L,"cpf");
        ordem.verify(medalhas).gerarMedalhaBronzePorPresenca(p);
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void RF_EVT_09_downloadDosDoisQrCodesRetornaPngComoAnexo(boolean inscricao) {
        Evento e=evento(CRIADO);
        when(eventos.procurarPorId(10L)).thenReturn(Optional.of(e)); when(qrClient.gerarQrCode(any())).thenReturn(new byte[]{1,2});
        var response=inscricao?controller.baixarQrCodeInscricao(10L):controller.baixarQrCodeConfirmacao(10L);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("attachment");
        assertThat(response.getBody()).containsExactly((byte)1,(byte)2);
        verify(qrClient).gerarQrCode(new QrCodeRequest(10L,"https://muttley.example.invalid",e.getTema(),
                inscricao ? TipoQrCode.INSCRICAO : TipoQrCode.CONFIRMACAO));
    }
    @Test void qrCodeSemUrlPersistidaTambemPodeSerGerado() {
        when(eventos.procurarPorId(10L)).thenReturn(Optional.of(evento(CRIADO)));
        when(qrClient.gerarQrCode(any())).thenReturn(new byte[]{1});
        assertThat(controller.baixarQrCodeInscricao(10L).getStatusCode().value()).isEqualTo(200);
        assertThat(controller.baixarQrCodeConfirmacao(10L).getStatusCode().value()).isEqualTo(200);
        verify(qrClient,times(2)).gerarQrCode(any());
    }
    @Test void falhaDoServicoQrCodeRetorna503SemVazarDetalheInterno() {
        when(eventos.procurarPorId(10L)).thenReturn(Optional.of(evento(CRIADO)));
        when(qrClient.gerarQrCode(any())).thenThrow(new RuntimeException("segredo interno"));
        assertThatThrownBy(() -> controller.baixarQrCodeInscricao(10L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(erro -> assertThat(((ResponseStatusException) erro).getStatusCode().value()).isEqualTo(503))
                .hasMessageNotContaining("segredo interno");
    }
}
