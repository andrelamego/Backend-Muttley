package com.fatec.muttley.auth;

import com.fatec.muttley.auth.dto.RegisterInfo;
import com.fatec.muttley.pessoa.AtualizacaoPessoa;
import com.fatec.muttley.pessoa.Pessoa;
import com.fatec.muttley.pessoa.PessoaRepository;
import com.fatec.muttley.pessoa.PessoaService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CadastroConviteService {
    private static final Duration VALIDADE = Duration.ofHours(24);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PessoaRepository pessoas;
    private final PessoaService pessoaService;
    private final Clock clock;

    @Transactional
    public Optional<String> emitir(Pessoa pessoa) {
        if (pessoa.getSenha() != null && !pessoa.getSenha().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cadastro já concluído.");
        }
        if (pessoa.getCadastroTokenHash() != null && pessoa.getCadastroTokenExpiraEm() != null
                && Instant.now(clock).isBefore(pessoa.getCadastroTokenExpiraEm())) {
            return Optional.empty();
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pessoa.setCadastroTokenHash(hash(token));
        pessoa.setCadastroTokenExpiraEm(Instant.now(clock).plus(VALIDADE));
        pessoas.save(pessoa);
        return Optional.of(token);
    }

    @Transactional(readOnly = true)
    public RegisterInfo consultar(String token) {
        Pessoa pessoa = pessoas.findByCadastroTokenHash(hash(token))
                .orElseThrow(this::conviteInvalido);
        validar(pessoa);
        return new RegisterInfo(pessoa.getNome(), pessoa.getEmail(), pessoa.getCpf());
    }

    @Transactional
    public Pessoa concluir(String token, AtualizacaoPessoa dados) {
        Pessoa pessoa = pessoas.findWithLockByCadastroTokenHash(hash(token))
                .orElseThrow(this::conviteInvalido);
        validar(pessoa);
        if (!pessoa.getEmail().equalsIgnoreCase(dados.email())
                || (pessoa.getCpf() != null && !apenasDigitos(pessoa.getCpf()).equals(apenasDigitos(dados.cpf())))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email e CPF devem corresponder ao convite.");
        }
        Pessoa salva = pessoaService.salvarOuAtualizar(dados.withId(pessoa.getId()));
        salva.setCadastroTokenHash(null);
        salva.setCadastroTokenExpiraEm(null);
        return pessoas.save(salva);
    }

    private void validar(Pessoa pessoa) {
        if (pessoa.getCadastroTokenExpiraEm() == null
                || !Instant.now(clock).isBefore(pessoa.getCadastroTokenExpiraEm())
                || (pessoa.getSenha() != null && !pessoa.getSenha().isBlank())) {
            throw conviteInvalido();
        }
    }

    private ResponseStatusException conviteInvalido() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite inválido ou expirado.");
    }

    private String apenasDigitos(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    private String hash(String token) {
        if (token == null || token.isBlank() || token.length() > 128) {
            throw conviteInvalido();
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
