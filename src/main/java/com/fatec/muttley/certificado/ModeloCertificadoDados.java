package com.fatec.muttley.certificado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Campos editáveis do modelo. A logo vai em base64, sem o prefixo "data:"; o tipo é descoberto pelo conteúdo.
 * As cores só aceitam #rrggbb porque entram no CSS do certificado.
 */
public record ModeloCertificadoDados(
        @NotBlank(message = "Informe o nome da instituição")
        @Size(max = 120, message = "Use até 120 caracteres")
        String instituicao,

        @NotBlank(message = "Informe o título")
        @Size(max = 80, message = "Use até 80 caracteres")
        String titulo,

        @NotBlank(message = "Informe o texto do certificado")
        @Size(max = 600, message = "Use até 600 caracteres")
        String texto,

        @NotBlank(message = "Informe o cargo de quem assina")
        @Size(max = 200, message = "Use até 200 caracteres")
        String cargoAssinatura,

        @NotBlank(message = "Informe a cor principal")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Use o formato #rrggbb")
        String corPrincipal,

        @NotBlank(message = "Informe a cor de destaque")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Use o formato #rrggbb")
        String corDestaque,

        @NotNull(message = "Escolha o leiaute")
        LeiauteCertificado leiaute,

        @Size(max = 700000, message = "A logo deve ter até 512 KB")
        String logoBase64,

        /** Só na resposta; na gravação o tipo vem do conteúdo da imagem. */
        String logoMime
) {
    public static ModeloCertificadoDados de(ModeloCertificado m) {
        return new ModeloCertificadoDados(m.getInstituicao(), m.getTitulo(), m.getTexto(), m.getCargoAssinatura(),
                m.getCorPrincipal(), m.getCorDestaque(), m.getLeiaute(), m.getLogoBase64(), m.getLogoMime());
    }
}
