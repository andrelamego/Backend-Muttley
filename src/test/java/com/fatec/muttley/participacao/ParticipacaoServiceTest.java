package com.fatec.muttley.participacao;

import com.fatec.muttley.evento.*;
import com.fatec.muttley.evento.enums.StatusEventoEnum;
import com.fatec.muttley.pessoa.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.Clock;
import java.util.*;
import static com.fatec.muttley.support.Cenarios.*;
import static com.fatec.muttley.evento.enums.StatusEventoEnum.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipacaoServiceTest {
    @Mock ParticipacaoRepository repository;
    @Mock NumeroInscricaoService numeros;
    @Mock PessoaService pessoas;
    @Mock EventoService eventos;
    @Spy ParticipacaoMapper mapper = Mappers.getMapper(ParticipacaoMapper.class);
    @Spy Clock clock = Clock.systemDefaultZone();
    @InjectMocks ParticipacaoService service;
    final InscricaoPublicaRequest request = new InscricaoPublicaRequest(" Nome novo ", " 529.982.247-25 ", " TESTE@EXAMPLE.INVALID ");
    private void eventoAberto() { when(eventos.procurarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(evento(CRIADO))); }
    private void janelaPresenca() {
        Evento e=evento(EM_ANDAMENTO);e.setData(LocalDate.now());e.setHorarioInicio("00:00");e.setHorarioFim("23:59");
        when(eventos.procurarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(e));
    }
    private void salvar() {
        when(pessoas.salvar(any())).thenAnswer(i -> { Pessoa p=i.getArgument(0); if(p.getId()==null)p.setId(1L); return p; });
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void RF_PAR_01_RN_PAR_06_07_08_09_criaCadastroParcialNormalizado() {
        eventoAberto(); salvar(); when(numeros.proximo()).thenReturn(42);
        Participacao salvo = service.registrarInscricaoPublica(10L, request);
        assertThat(salvo.getInscricao()).isEqualTo(42);
        assertThat(salvo.getTipo()).isEqualTo("Participante"); assertThat(salvo.isPresente()).isFalse();
        assertThat(salvo.getPessoa().getNome()).isEqualTo("Nome novo");
        assertThat(salvo.getPessoa().getEmail()).isEqualTo("teste@example.invalid");
        assertThat(salvo.getPessoa().getCpf()).isEqualTo("529.982.247-25");
        assertThat(salvo.getPessoa().getRole()).isEqualTo(Role.USER);
        assertThat(salvo.getPessoa().getSenha()).isNull(); assertThat(salvo.getPessoa().getTelefone()).isNull();
    }
    @ParameterizedTest @ValueSource(strings={"cpf","email"})
    void RN_PAR_02_03_reutilizaPessoaSemSobrescreverCadastro(String chave) {
        eventoAberto(); salvar(); Pessoa existente = pessoa(1L); existente.setRole(Role.ADMIN);
        if(chave.equals("cpf")) when(pessoas.procurarPorCpf("529.982.247-25")).thenReturn(Optional.of(existente));
        else when(pessoas.procurarPorEmail("teste@example.invalid")).thenReturn(Optional.of(existente));
        Participacao salvo = service.registrarInscricaoPublica(10L, request);
        assertThat(salvo.getPessoa()).isSameAs(existente);
        assertThat(existente.getNome()).isEqualTo("Participante de teste");
        assertThat(existente.getEmail()).isEqualTo("pessoa1@example.invalid");
        assertThat(existente.getRole()).as("Inscricao nao deve remover privilegios de pessoa existente").isEqualTo(Role.ADMIN);
    }
    @Test void RN_PAR_04_rejeitaIdentidadesConflitantesSemSalvar() {
        eventoAberto(); when(pessoas.procurarPorCpf("529.982.247-25")).thenReturn(Optional.of(pessoa(1)));
        when(pessoas.procurarPorEmail("teste@example.invalid")).thenReturn(Optional.of(pessoa(2)));
        assertStatus(409, () -> service.registrarInscricaoPublica(10L, request));
        verify(pessoas,never()).salvar(any()); verify(repository,never()).save(any());
    }
    @Test void RN_PAR_05_rejeitaInscricaoDuplicada() {
        eventoAberto(); Pessoa p=pessoa(1);
        when(pessoas.procurarPorCpf("529.982.247-25")).thenReturn(Optional.of(p));
        when(pessoas.salvar(p)).thenReturn(p);
        when(repository.existsByEventoIdAndPessoaId(10L,1L)).thenReturn(true);
        assertStatus(409, () -> service.registrarInscricaoPublica(10L, request));
        verify(repository,never()).save(any());
    }
    @ParameterizedTest @EnumSource(value=StatusEventoEnum.class,names={"EM_ANDAMENTO","CANCELADO","FINALIZADO"})
    void RN_PAR_01_rejeitaEstadoFechado(StatusEventoEnum status) {
        when(eventos.procurarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(evento(status)));
        assertStatus(400, () -> service.registrarInscricaoPublica(10L,request));
        verifyNoInteractions(pessoas,repository);
    }
    @Test void RN_PAR_01_rejeitaCriadoComInicioNoPassado() {
        Evento e=evento(CRIADO); e.setData(LocalDate.now().minusDays(1));
        when(eventos.procurarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(e));
        assertStatus(400, () -> service.registrarInscricaoPublica(10L,request));
        verifyNoInteractions(pessoas,repository);
    }
    @Test void eventoInexistenteRetorna404() {
        assertStatus(404, () -> service.registrarInscricaoPublica(10L,request));
        verifyNoInteractions(pessoas,repository);
    }
    @Test void RF_PAR_04_confirmaPresencaDeInscrito() {
        janelaPresenca();
        Participacao p=participacao(1,false);
        when(pessoas.procurarPorCpf("cpf")).thenReturn(Optional.of(p.getPessoa()));
        when(repository.existsByEventoIdAndPessoaId(10L,1L)).thenReturn(true);
        when(repository.findByEventoIdAndPessoaId(10L,1L)).thenReturn(Optional.of(p));
        when(repository.save(p)).thenReturn(p);
        assertThat(service.confirmarPresenca(10L,"cpf").isPresente()).isTrue();
        verify(repository).save(p);
    }
    @Test void presencaDuplicadaRetorna409SemNovaGravacao() {
        janelaPresenca();
        Participacao p=participacao(1,true);
        when(pessoas.procurarPorCpf("cpf")).thenReturn(Optional.of(p.getPessoa()));
        when(repository.existsByEventoIdAndPessoaId(10L,1L)).thenReturn(true);
        when(repository.findByEventoIdAndPessoaId(10L,1L)).thenReturn(Optional.of(p));
        assertStatus(409, () -> service.confirmarPresenca(10L,"cpf"));
        verify(repository,never()).save(any());
    }
    @Test void presencaSemInscricaoRetorna404() {
        janelaPresenca();
        when(pessoas.procurarPorCpf("cpf")).thenReturn(Optional.of(pessoa(1)));
        assertStatus(404, () -> service.confirmarPresenca(10L,"cpf"));
        verify(repository,never()).save(any());
    }
    @Test void presencaSemPessoaFalhaSemGravar() {
        janelaPresenca();
        assertThatThrownBy(() -> service.confirmarPresenca(10L,"cpf")).isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(repository);
    }
    @Test void RN_PAR_12_crudExigePessoaExistente() {
        assertThatThrownBy(() -> service.salvarOuAtualizar(new AtualizacaoParticipacao(null,1,"Participante",1L,10L)))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(repository,eventos);
    }
    @Test void RN_PAR_12_crudExigeEventoExistente() {
        when(pessoas.procurarPorId(1L)).thenReturn(Optional.of(pessoa(1)));
        assertThatThrownBy(() -> service.salvarOuAtualizar(new AtualizacaoParticipacao(null,1,"Participante",1L,10L)))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(repository);
    }
    private static void assertStatus(int status, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode().value()).isEqualTo(status));
    }
    @Test void RF_PAR_05_edicaoPreservaPresencaEIdEResolveVinculos() {
        var p=pessoa(1);var e=evento(EM_ANDAMENTO);var existente=participacao(7,true);
        when(pessoas.procurarPorId(1L)).thenReturn(Optional.of(p));when(eventos.procurarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(e));
        when(repository.findById(7L)).thenReturn(Optional.of(existente));when(repository.save(existente)).thenReturn(existente);
        var salva=service.salvarOuAtualizar(new AtualizacaoParticipacao(7L,42,"Palestrante",1L,10L));
        assertThat(salva.getId()).isEqualTo(7L);assertThat(salva.isPresente()).isTrue();assertThat(salva.getTipo()).isEqualTo("Palestrante");
        assertThat(salva.getInscricao()).isEqualTo(42);assertThat(salva.getPessoa()).isSameAs(p);assertThat(salva.getEvento()).isSameAs(e);
    }
}
