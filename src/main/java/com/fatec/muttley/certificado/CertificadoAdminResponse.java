package com.fatec.muttley.certificado;

import com.fatec.muttley.evento.Evento;
import com.fatec.muttley.participacao.Participacao;
import com.fatec.muttley.pessoa.Pessoa;
import java.time.LocalDate;

/**
 * Certificado na listagem da coordenação: quem recebeu e de qual evento.
 * A entidade não serve aqui porque o evento da participação é omitido do JSON
 * e a pessoa levaria CPF e telefone junto.
 */
public record CertificadoAdminResponse(
        Long id,
        LocalDate dataEmissao,
        String codigoValidacao,
        String urlPublica,
        ParticipacaoAdmin participacao) {

    public record ParticipacaoAdmin(String tipo, PessoaAdmin pessoa, EventoAdmin evento) {}

    public record PessoaAdmin(Long id, String nome, String email) {}

    public record EventoAdmin(Long id, String tema, LocalDate data) {}

    public static CertificadoAdminResponse de(Certificado certificado) {
        Participacao participacao = certificado.getParticipacao();
        ParticipacaoAdmin participacaoAdmin = null;
        if (participacao != null) {
            Pessoa pessoa = participacao.getPessoa();
            Evento evento = participacao.getEvento();
            participacaoAdmin = new ParticipacaoAdmin(
                    participacao.getTipo(),
                    pessoa != null ? new PessoaAdmin(pessoa.getId(), pessoa.getNome(), pessoa.getEmail()) : null,
                    evento != null ? new EventoAdmin(evento.getId(), evento.getTema(), evento.getData()) : null);
        }
        return new CertificadoAdminResponse(
                certificado.getId(),
                certificado.getDataEmissao(),
                certificado.getCodigoValidacao(),
                certificado.getUrlPublica(),
                participacaoAdmin);
    }
}
