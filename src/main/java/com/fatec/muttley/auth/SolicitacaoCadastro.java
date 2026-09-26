package com.fatec.muttley.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitacaoCadastro(
        @NotBlank String nome,
        @NotBlank @Email String email
) {
}
