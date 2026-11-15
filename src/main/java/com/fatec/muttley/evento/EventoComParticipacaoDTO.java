package com.fatec.muttley.evento;

import jakarta.validation.Valid;

import java.util.List;

/**
 * Evento e quem palestra nele. palestrantes são ids de pessoas: o backend cria,
 * mantém ou remove as participações do tipo "Palestrante" para bater com a lista.
 * Nulo deixa os palestrantes como estão; lista vazia remove todos.
 */
public record EventoComParticipacaoDTO(
        @Valid AtualizacaoEvento evento,
        List<Long> palestrantes
) {}
