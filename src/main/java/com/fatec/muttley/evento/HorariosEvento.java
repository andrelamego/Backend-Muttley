package com.fatec.muttley.evento;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class HorariosEvento {
    private static final DateTimeFormatter HORA_MINUTO = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private HorariosEvento() {
    }

    public static boolean jaIniciou(Evento evento, Clock clock) {
        if (evento.getData() == null || evento.getHorarioInicio() == null || evento.getHorarioInicio().isBlank()) {
            return false;
        }

        try {
            LocalDateTime inicio = LocalDateTime.of(
                    evento.getData(), LocalTime.parse(evento.getHorarioInicio(), HORA_MINUTO));
            return !inicio.isAfter(LocalDateTime.now(clock));
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}
