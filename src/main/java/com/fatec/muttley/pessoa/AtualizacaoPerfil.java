package com.fatec.muttley.pessoa;

import jakarta.validation.constraints.NotBlank;

/** Dados que a própria pessoa pode alterar. Email (login) e CPF ficam com a coordenação. */
public record AtualizacaoPerfil(
        @NotBlank(message = "Nome e obrigatorio")
        String nome,

        @NotBlank(message = "Telefone e obrigatorio")
        String telefone
) {
}
