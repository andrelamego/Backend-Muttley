package com.fatec.muttley.evento;

import com.fatec.muttley.evento.enums.ModalidadeEventoEnum;
import com.fatec.muttley.evento.enums.StatusEventoEnum;
import java.time.LocalDate;

/** Dados de agenda do painel, sem proxies JPA nem dados pessoais de participantes. */
public record EventoResumoResponse(Long id, String tema, String descricao, LocalDate data,
                                  String horarioInicio, String horarioFim, ModalidadeEventoEnum modalidade,
                                  StatusEventoEnum status, String disciplina, String local) {
    public static EventoResumoResponse from(Evento evento) {
        return new EventoResumoResponse(evento.getId(), evento.getTema(), evento.getDescricao(), evento.getData(),
                evento.getHorarioInicio(), evento.getHorarioFim(), evento.getModalidade(), evento.getStatus(),
                evento.getDisciplina() == null ? null : evento.getDisciplina().getNome(),
                evento.getLocal() == null ? null : evento.getLocal().getNome());
    }
}
