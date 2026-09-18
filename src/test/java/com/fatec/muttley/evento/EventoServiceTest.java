package com.fatec.muttley.evento;

import com.fatec.muttley.disciplina.*;
import com.fatec.muttley.local.*;
import com.fatec.muttley.patrocinador.*;
import com.fatec.muttley.evento.enums.StatusEventoEnum;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.util.*;
import static com.fatec.muttley.support.Cenarios.*;
import static com.fatec.muttley.evento.enums.StatusEventoEnum.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {
    @Mock EventoRepository repository;
    @Mock DisciplinaService disciplinas;
    @Mock PatrocinadorService patrocinadores;
    @Mock LocalService locais;
    @Spy EventoMapper mapper = Mappers.getMapper(EventoMapper.class);
    @InjectMocks EventoService service;

    private void referencias() {
        when(disciplinas.procurarPorId(1L)).thenReturn(Optional.of(new Disciplina()));
        when(patrocinadores.procurarPorId(2L)).thenReturn(Optional.of(new Patrocinador()));
        when(locais.procurarPorId(3L)).thenReturn(Optional.of(new Local()));
    }
    private void salvar() { when(repository.save(any())).thenAnswer(i -> i.getArgument(0)); }

    @Test void RN_EVT_01_criacaoIgnoraStatusRecebido() {
        referencias(); salvar();
        Evento salvo = service.salvarOuAtualizar(dadosEvento(null, "09:00", "11:00"));
        assertThat(salvo.getId()).isNull();
        assertThat(salvo.getStatus()).isEqualTo(CRIADO);
        assertThat(salvo.getDisciplina()).isNotNull();
        assertThat(salvo.getPatrocinador()).isNotNull();
        assertThat(salvo.getLocal()).isNotNull();
        assertThat(salvo.getTema()).isEqualTo("Semana de tecnologia");
    }
    @ParameterizedTest @CsvSource({"11:00,10:00", "09:00,09:00", "9h,11:00", "09:00,25:00", "24:00,11:00", "09:00,24:00"})
    void RN_EVT_03_04_rejeitaHorarioInvalido(String inicio, String fim) {
        assertThatThrownBy(() -> service.salvarOuAtualizar(dadosEvento(null, inicio, fim)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
    @Test void RN_EVT_05_rejeitaDisciplinaInexistente() {
        assertThatThrownBy(() -> service.salvarOuAtualizar(dadosEvento(null,"09:00","10:00")))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(repository, patrocinadores, locais);
    }
    @Test void RN_EVT_05_rejeitaPatrocinadorInexistente() {
        when(disciplinas.procurarPorId(1L)).thenReturn(Optional.of(new Disciplina()));
        assertThatThrownBy(() -> service.salvarOuAtualizar(dadosEvento(null,"09:00","10:00")))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(repository, locais);
    }
    @Test void RN_EVT_05_rejeitaLocalInexistente() {
        when(disciplinas.procurarPorId(1L)).thenReturn(Optional.of(new Disciplina()));
        when(patrocinadores.procurarPorId(2L)).thenReturn(Optional.of(new Patrocinador()));
        assertThatThrownBy(() -> service.salvarOuAtualizar(dadosEvento(null,"09:00","10:00")))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(repository);
    }
    @Test void RN_EVT_06_finalizadoNaoPodeSerEditado() {
        referencias(); when(repository.findByIdParaAtualizacao(10L)).thenReturn(Optional.of(evento(FINALIZADO)));
        assertThatThrownBy(() -> service.salvarOuAtualizar(dadosEvento(10L,"09:00","10:00")))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }
    @Test void RF_EVT_04_edicaoPreservaIdentidadeStatusEQrCodes() {
        referencias(); salvar(); Evento existente = evento(EM_ANDAMENTO);
        existente.setQrCodeInscricaoUrl("https://example.invalid/qr");
        when(repository.findByIdParaAtualizacao(10L)).thenReturn(Optional.of(existente));
        Evento resultado = service.salvarOuAtualizar(dadosEvento(10L,"10:00","12:00"));
        assertThat(resultado).isSameAs(existente);
        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getStatus()).isEqualTo(EM_ANDAMENTO);
        assertThat(resultado.getHorarioInicio()).isEqualTo("10:00");
        assertThat(resultado.getQrCodeInscricaoUrl()).isEqualTo("https://example.invalid/qr");
    }
    @ParameterizedTest @EnumSource(value=StatusEventoEnum.class, names={"FINALIZADO","CANCELADO"})
    void RN_EVT_07_08_rejeitaCancelamento(StatusEventoEnum status) {
        when(repository.findByIdParaAtualizacao(10L)).thenReturn(Optional.of(evento(status)));
        assertThatThrownBy(() -> service.cancelarEvento(10L)).isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }
    @ParameterizedTest @EnumSource(value=StatusEventoEnum.class, names={"CRIADO","EM_ANDAMENTO"})
    void RF_EVT_05_cancelaEventoPermitido(StatusEventoEnum status) {
        Evento evento = evento(status); when(repository.findByIdParaAtualizacao(10L)).thenReturn(Optional.of(evento));
        service.cancelarEvento(10L);
        assertThat(evento.getStatus()).isEqualTo(CANCELADO); verify(repository).save(evento);
    }
    @ParameterizedTest @EnumSource(value=StatusEventoEnum.class, names={"CRIADO","FINALIZADO","CANCELADO"})
    void RN_EVT_09_rejeitaConclusaoForaDeAndamento(StatusEventoEnum status) {
        when(repository.findByIdParaAtualizacao(10L)).thenReturn(Optional.of(evento(status)));
        assertThatThrownBy(() -> service.concluirEvento(10L)).isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }
    @Test void RN_EVT_09_concluiEventoEmAndamento() {
        Evento evento = evento(EM_ANDAMENTO); when(repository.findByIdParaAtualizacao(10L)).thenReturn(Optional.of(evento));
        service.concluirEvento(10L);
        assertThat(evento.getStatus()).isEqualTo(FINALIZADO); verify(repository).save(evento);
    }
    @Test void RN_EVT_10_iniciaEventoQueJaComecou() {
        salvar(); Evento evento = evento(CRIADO); evento.setData(LocalDate.now().minusDays(1));
        when(repository.findById(10L)).thenReturn(Optional.of(evento));
        assertThat(service.procurarPorId(10L).orElseThrow().getStatus()).isEqualTo(EM_ANDAMENTO);
        verify(repository).save(evento);
    }
    @ParameterizedTest @EnumSource(value=StatusEventoEnum.class, names={"FINALIZADO","CANCELADO","EM_ANDAMENTO"})
    void RN_EVT_10_naoReabreEstadoExistente(StatusEventoEnum status) {
        Evento evento = evento(status); evento.setData(LocalDate.now().minusDays(1));
        when(repository.findById(10L)).thenReturn(Optional.of(evento));
        assertThat(service.procurarPorId(10L).orElseThrow().getStatus()).isEqualTo(status);
        verify(repository, never()).save(any());
    }
    @Test void RN_EVT_11_listaPublicaRemoveEventosQueAcabaramDeIniciar() {
        salvar(); Evento futuro = evento(CRIADO), passado = evento(CRIADO);
        passado.setData(LocalDate.now().minusDays(1));
        when(repository.findByStatusOrderByDataAscHorarioInicioAsc(CRIADO)).thenReturn(List.of(passado, futuro));
        assertThat(service.procurarDisponiveisParaInscricao()).containsExactly(futuro);
    }
    @Test void RN_EVT_12_RF_EVT_07_passaFiltrosEPaginacaoSemCancelados() {
        Pageable page = PageRequest.of(2,5,Sort.by("tema"));
        when(repository.findProximosEventosFiltrados(anyList(),isNull(),eq(""),eq(page))).thenReturn(Page.empty(page));
        assertThat(service.procurarProximosFiltrados(null,null,page).getNumber()).isEqualTo(2);
        verify(repository).findProximosEventosFiltrados(List.of(CRIADO,EM_ANDAMENTO,FINALIZADO),null,"",page);
    }
    @Test void RF_EVT_09_persisteQrCodesSeparados() {
        Evento evento = evento(CRIADO); when(repository.findByIdParaAtualizacao(10L)).thenReturn(Optional.of(evento));
        service.salvarQrCodeInscricaoUrl(10L,"inscricao"); service.salvarQrCodeConfirmacaoUrl(10L,"presenca");
        assertThat(evento.getQrCodeInscricaoUrl()).isEqualTo("inscricao");
        assertThat(evento.getQrCodeConfirmacaoUrl()).isEqualTo("presenca");
    }
}
