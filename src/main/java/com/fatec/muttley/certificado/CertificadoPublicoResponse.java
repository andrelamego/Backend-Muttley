package com.fatec.muttley.certificado;

import com.fatec.muttley.evento.Evento;
import com.fatec.muttley.evento.enums.ModalidadeEventoEnum;
import com.fatec.muttley.participacao.Participacao;
import java.time.LocalDate;

/**
 * Dados do certificado que qualquer pessoa pode ver pelo código de validação.
 * Expõe só o nome do participante e o evento; CPF, e-mail e telefone ficam de fora.
 */
public record CertificadoPublicoResponse(
        String codigoValidacao,
        LocalDate dataEmissao,
        String assinatura,
        String urlPublica,
        ParticipacaoPublica participacao) {

    public record ParticipacaoPublica(String tipo, PessoaPublica pessoa, EventoPublico evento) {}

    public record PessoaPublica(String nome) {}

    public record EventoPublico(
            String tema,
            LocalDate data,
            String horarioInicio,
            String horarioFim,
            ModalidadeEventoEnum modalidade) {}

    public static CertificadoPublicoResponse de(Certificado certificado) {
        Participacao participacao = certificado.getParticipacao();
        ParticipacaoPublica participacaoPublica = null;
        if (participacao != null) {
            Evento evento = participacao.getEvento();
            participacaoPublica = new ParticipacaoPublica(
                    participacao.getTipo(),
                    participacao.getPessoa() != null ? new PessoaPublica(participacao.getPessoa().getNome()) : null,
                    evento != null
                            ? new EventoPublico(evento.getTema(), evento.getData(), evento.getHorarioInicio(),
                                    evento.getHorarioFim(), evento.getModalidade())
                            : null);
        }
        return new CertificadoPublicoResponse(
                certificado.getCodigoValidacao(),
                certificado.getDataEmissao(),
                certificado.getAssinatura(),
                certificado.getUrlPublica(),
                participacaoPublica);
    }
}
