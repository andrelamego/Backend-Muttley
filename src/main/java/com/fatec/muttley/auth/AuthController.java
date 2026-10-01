package com.fatec.muttley.auth;

import com.fatec.muttley.pessoa.AtualizacaoPessoa;
import com.fatec.muttley.pessoa.Pessoa;
import com.fatec.muttley.pessoa.PessoaService;
import com.fatec.muttley.pessoa.Role;
import com.fatec.muttley.email.EmailProducer;
import com.fatec.muttley.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Autenticação", description = "Endpoints de autenticação, registro de usuários e emissão de tokens JWT")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PessoaService pessoaService;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final JwtDecoder jwtDecoder;
    private final CadastroConviteService cadastroConviteService;
    private final EmailProducer emailProducer;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public record LoginRequest(
            @NotBlank(message = "Email e obrigatorio")
            @Email(message = "Email invalido")
            String email,

            @NotBlank(message = "Senha e obrigatoria")
            String senha
    ) {
    }

    public record UsuarioResponse(Long id, String nome, String email, Role role) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UsuarioResponse usuario) {
    }

    @Operation(summary = "Solicitar cadastro", description = "Cria uma conta USER pendente e envia um convite para definir a senha.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitação recebida"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos")
    })
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> cadastrarUsuario(@RequestBody @Valid SolicitacaoCadastro dto) {
        Optional<Pessoa> existente = pessoaService.procurarPorEmail(dto.email());
        if (existente.isPresent()) {
            if (existente.get().getSenha() == null) {
                cadastroConviteService.emitir(existente.get())
                        .ifPresent(token -> emailProducer.publicarCompletarCadastro(existente.get(), frontendUrl, token));
            }
        } else {
            Pessoa pessoa = new Pessoa();
            pessoa.setNome(dto.nome());
            pessoa.setEmail(dto.email());
            pessoa.setRole(Role.USER);
            Pessoa salva = pessoaService.salvar(pessoa);
            cadastroConviteService.emitir(salva)
                    .ifPresent(token -> emailProducer.publicarCompletarCadastro(salva, frontendUrl, token));
        }
        return ResponseEntity.accepted().body(Map.of("message", "Se os dados estiverem corretos, enviaremos um convite por email."));
    }

    @Operation(summary = "Completar cadastro", description = "Completa o cadastro com o convite de uso único enviado por email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadastro completado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convite inválido ou expirado")
    })
    @PutMapping("/register")
    public ResponseEntity<?> completarCadastro(@RequestParam("token") String token,
            @RequestBody @Valid AtualizacaoPessoa dto) {
        return ResponseEntity.ok(usuarioResponse(cadastroConviteService.concluir(token, dto)));
    }

    @Operation(summary = "Autenticar usuário (Login)", description = "Valida as credenciais de login e retorna o token JWT de acesso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email ou senha inválidos")
    })
    @PostMapping("/login")
    public ResponseEntity<?> validarCredenciais(@RequestBody @Valid LoginRequest loginRequest) {
        Pessoa pessoaSalva = pessoaService.procurarPorEmail(loginRequest.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos."));
        if (pessoaSalva.getSenha() == null || !passwordEncoder.matches(loginRequest.senha(), pessoaSalva.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos.");
        }

        if (pessoaSalva.getRole() == null) {
            pessoaSalva.setRole(Role.USER);
            pessoaSalva = pessoaService.salvar(pessoaSalva);
        }

        return ResponseEntity.ok(new LoginResponse(
                jwtService.gerarToken(pessoaSalva),
                "Bearer",
                jwtService.getExpirationSeconds(),
                usuarioResponse(pessoaSalva)
        ));
    }

    private UsuarioResponse usuarioResponse(Pessoa pessoa) {
        return new UsuarioResponse(pessoa.getId(), pessoa.getNome(), pessoa.getEmail(), pessoa.getRole());
    }
}
