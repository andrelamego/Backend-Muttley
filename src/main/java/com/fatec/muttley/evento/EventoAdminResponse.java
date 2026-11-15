package com.fatec.muttley.evento;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

/** Evento para edição na coordenação: os campos do formulário e os palestrantes já vinculados. */
public record EventoAdminResponse(
        @JsonUnwrapped AtualizacaoEvento evento,
        List<PalestranteResumo> palestrantes
) {
    public record PalestranteResumo(Long id, String nome) {}
}
