package com.fatec.muttley.painel;

import com.fatec.muttley.evento.enums.StatusEventoEnum;
import java.time.LocalDate;

/**
 * Um evento com as contagens que o painel da coordenação usa. Inscritos e
 * presentes não contam palestrantes: eles não ocupam vaga nem entram na taxa.
 */
public record EstatisticaEvento(
        Long id,
        String tema,
        LocalDate data,
        String horarioInicio,
        String horarioFim,
        StatusEventoEnum status,
        String disciplina,
        Integer capacidade,
        Long inscritos,
        Long presentes) {

    public long inscritosOuZero() {
        return inscritos == null ? 0 : inscritos;
    }

    public long presentesOuZero() {
        return presentes == null ? 0 : presentes;
    }
}
