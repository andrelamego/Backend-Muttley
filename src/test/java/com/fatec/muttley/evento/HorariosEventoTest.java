package com.fatec.muttley.evento;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HorariosEventoTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));

    @ParameterizedTest
    @CsvSource({"08:59,true", "09:00,true", "09:01,false", "24:00,false", "invalido,false"})
    void inicioDoEventoDefineEncerramentoDaInscricao(String horario, boolean esperado) {
        Evento evento = new Evento();
        evento.setData(LocalDate.of(2026, 9, 25));
        evento.setHorarioInicio(horario);

        assertThat(HorariosEvento.jaIniciou(evento, clock)).isEqualTo(esperado);
    }

    @Test
    void dadosDeHorarioIncompletosNaoIndicamInicio() {
        Evento evento = new Evento();
        assertThat(HorariosEvento.jaIniciou(evento, clock)).isFalse();

        evento.setData(LocalDate.of(2026, 9, 25));
        evento.setHorarioInicio(" ");
        assertThat(HorariosEvento.jaIniciou(evento, clock)).isFalse();
    }
}
