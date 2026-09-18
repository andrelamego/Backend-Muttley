package com.fatec.muttley.auth;

import com.fatec.muttley.auth.dto.RegisterInfo;
import com.fatec.muttley.pessoa.AtualizacaoPessoa;
import com.fatec.muttley.pessoa.Pessoa;
import com.fatec.muttley.pessoa.PessoaService;
import com.fatec.muttley.pessoa.Role;
import com.fatec.muttley.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Endpoints de autenticação, registro de usuários e emissão de tokens JWT")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private PessoaService pessoaService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final JwtDecoder jwtDecoder;

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

    @Operation(summary = "Registrar novo usuário", description = "Cadastra uma nova pessoa no sistema. Caso seja o primeiro cadastro, assume o papel de ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Email já cadastrado")
    })
    @PostMapping("/register")
    public ResponseEntity<?> cadastrarUsuario(@RequestBody @Valid AtualizacaoPessoa dto) {
        try {
            if (pessoaService.existePorEmail(dto.email())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Usuario ja cadastrado com esse email."));
            }

            boolean criarAdmin = !pessoaService.existeAdmin();
            Pessoa pessoaSalva = pessoaService.salvarOuAtualizar(dto.withId(null));
            pessoaSalva.setRole(criarAdmin ? Role.ADMIN : Role.USER);
            pessoaSalva = pessoaService.salvar(pessoaSalva);

            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioResponse(pessoaSalva));
        } catch (EntityNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", exception.getMessage()));
        }
    }

    @Operation(summary = "Completar cadastro", description = "Completa o cadastro de uma pessoa previamente registrada definindo a senha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadastro completado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Cadastro já completado anteriormente")
    })
    @PutMapping("/register")
    public ResponseEntity<?> completarCadastro(@RequestBody @Valid AtualizacaoPessoa dto) {
        try {
            Pessoa pessoa = pessoaService.procurarPorEmail(dto.email())
                    .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado com esse email."));

            if (StringUtils.hasText(pessoa.getSenha())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Cadastro ja foi completado anteriormente."));
            }

            dto = dto.withId(pessoa.getId());

            Pessoa pessoaSalva = pessoaService.salvarOuAtualizar(dto);

            return ResponseEntity.ok(usuarioResponse(pessoaSalva));
        } catch (EntityNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", exception.getMessage()));
        }
    }

    @Operation(summary = "Autenticar usuário (Login)", description = "Valida as credenciais de login e retorna o token JWT de acesso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email ou senha inválidos")
    })
    @PostMapping("/login")
    public ResponseEntity<?> validarCredenciais(@RequestBody @Valid LoginRequest loginRequest) {
        try {
            Optional<Pessoa> pessoaOptional = pessoaService.procurarPorEmail(loginRequest.email());

            if (pessoaOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Email ou senha invalidos"));
            }

            Pessoa pessoaSalva = pessoaOptional.get();

            if (!passwordEncoder.matches(loginRequest.senha(), pessoaSalva.getSenha())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Email ou senha invalidos"));
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
        } catch (EntityNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", exception.getMessage()));
        }
    }

    private UsuarioResponse usuarioResponse(Pessoa pessoa) {
        return new UsuarioResponse(pessoa.getId(), pessoa.getNome(), pessoa.getEmail(), pessoa.getRole());
    }
}
