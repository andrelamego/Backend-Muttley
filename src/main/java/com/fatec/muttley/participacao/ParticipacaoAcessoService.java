package com.fatec.muttley.participacao;

import com.fatec.muttley.pessoa.PessoaService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
public class ParticipacaoAcessoService {
    private final PessoaService pessoas;
    private final ParticipacaoService participacoes;

    public ParticipacaoAcessoService(PessoaService pessoas, ParticipacaoService participacoes) {
        this.pessoas = pessoas;
        this.participacoes = participacoes;
    }

    public List<Participacao> listar() {
        return administrador() ? participacoes.procurarTodos() : participacoes.procurarPorPessoa(pessoaId());
    }

    public void validarPessoa(Long id) {
        if (!administrador() && !pessoaId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você só pode administrar suas próprias participações.");
        }
    }

    public void validarParticipacao(Participacao participacao) {
        validarPessoa(participacao.getPessoa().getId());
    }

    private Authentication autenticacao() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação obrigatória.");
        }
        return auth;
    }

    private boolean administrador() {
        return autenticacao().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Long pessoaId() {
        String email = ((Jwt) autenticacao().getPrincipal()).getSubject();
        return pessoas.procurarPorEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado não encontrado.")).getId();
    }
}
