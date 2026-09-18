package com.fatec.muttley.pessoa;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.andrelamego.brValidator.cpf.ValidCpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AtualizacaoPessoa(
        Long id,
        @NotBlank(message = "Nome e obrigatorio")
        String nome,

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,

        @NotBlank(message = "Telefone e obrigatorio")
        String telefone,

        @NotBlank(message = "CPF e obrigatorio")
        @ValidCpf(formatted = true)
        String cpf,

        @NotBlank(message = "Senha e obrigatoria")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String senha
) {
        public AtualizacaoPessoa withId(Long id) {
                return new AtualizacaoPessoa(id, this.nome(), this.email(), this.telefone(), this.cpf(), this.senha());
        }
}
