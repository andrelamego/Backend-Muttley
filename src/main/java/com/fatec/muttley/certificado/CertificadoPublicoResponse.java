package com.fatec.muttley.certificado;

import com.fatec.muttley.evento.Evento;
import com.fatec.muttley.participacao.Participacao;
import java.time.LocalDate;

// Resposta do link público: só o que a página de validação mostra, sem CPF, e-mail ou telefone.
public record CertificadoPublicoResponse(Long id, String codigoValidacao, LocalDate dataEmissao, String assinatura,
                                         ParticipacaoPublica participacao) {

    public record ParticipacaoPublica(int inscricao, String tipo, PessoaPublica pessoa, EventoPublico evento) {}

    public record PessoaPublica(String nome) {}

    public record EventoPublico(String tema, LocalDate data, String horarioInicio, String horarioFim) {}

    public static CertificadoPublicoResponse de(Certificado certificado) {
        Participacao p = certificado.getParticipacao();
        ParticipacaoPublica participacao = null;
        if (p != null) {
            Evento e = p.getEvento();
            participacao = new ParticipacaoPublica(p.getInscricao(), p.getTipo(),
                    p.getPessoa() == null ? null : new PessoaPublica(p.getPessoa().getNome()),
                    e == null ? null : new EventoPublico(e.getTema(), e.getData(), e.getHorarioInicio(), e.getHorarioFim()));
        }
        return new CertificadoPublicoResponse(certificado.getId(), certificado.getCodigoValidacao(),
                certificado.getDataEmissao(), certificado.getAssinatura(), participacao);
    }
}
