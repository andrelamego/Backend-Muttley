package com.fatec.muttley.pessoa;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AlteracaoSenha(
        @NotBlank(message = "Informe a senha atual")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String senhaAtual,

        @NotBlank(message = "Informe a nova senha")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String novaSenha
) {
}
