package com.fatec.muttley.pessoa;

import com.fatec.muttley.certificado.CertificadoService;
import com.fatec.muttley.medalha.MedalhaService;
import com.fatec.muttley.participacao.ParticipacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Meu Perfil", description = "Endpoints de consulta exclusivos do usuário autenticado")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class MeController {

    private final PessoaService pessoaService;

    private final CertificadoService certificadoService;

    private final MedalhaService medalhaService;

    private final ParticipacaoService participacaoService;

    @Operation(summary = "Obter dados do usuário autenticado", description = "Retorna os dados cadastrais da pessoa vinculada ao token JWT atual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados obtidos com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado ou token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/api/me")
    public ResponseEntity<PessoaMeResponse> buscarUsuarioAutenticado(JwtAuthenticationToken authentication) {
        Pessoa pessoa = pessoaAutenticada(authentication);
        return ResponseEntity.ok(PessoaMeResponse.from(pessoa));
    }

    @Operation(summary = "Listar certificados do usuário autenticado", description = "Retorna todos os certificados emitidos para o usuário logado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de certificados do usuário"),
            @ApiResponse(responseCode = "401", description = "Não autenticado ou token inválido")
    })
    @GetMapping("/api/me/certificados")
    public ResponseEntity<List<CertificadoUsuarioResponse>> listarCertificados(JwtAuthenticationToken authentication) {
        Pessoa pessoa = pessoaAutenticada(authentication);
        List<CertificadoUsuarioResponse> certificados = certificadoService.procurarPorPessoa(pessoa.getId()).stream()
                .map(CertificadoUsuarioResponse::from)
                .toList();
        return ResponseEntity.ok(certificados);
    }

    @Operation(summary = "Listar medalhas do usuário autenticado", description = "Retorna todas as medalhas e conquistas obtidas pelo usuário logado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de medalhas do usuário"),
            @ApiResponse(responseCode = "401", description = "Não autenticado ou token inválido")
    })
    @GetMapping("/api/me/medalhas")
    public ResponseEntity<List<MedalhaUsuarioResponse>> listarMedalhas(JwtAuthenticationToken authentication) {
        Pessoa pessoa = pessoaAutenticada(authentication);
        List<MedalhaUsuarioResponse> medalhas = medalhaService.procurarPorPessoa(pessoa.getId()).stream()
                .map(MedalhaUsuarioResponse::from)
                .toList();
        return ResponseEntity.ok(medalhas);
    }

    @Operation(summary = "Listar participações do usuário autenticado", description = "Retorna o histórico de inscrições e presenças em eventos do usuário logado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de participações do usuário"),
            @ApiResponse(responseCode = "401", description = "Não autenticado ou token inválido")
    })
    @GetMapping("/api/me/participacoes")
    public ResponseEntity<List<ParticipacaoUsuarioResponse>> listarParticipacoes(JwtAuthenticationToken authentication) {
        Pessoa pessoa = pessoaAutenticada(authentication);
        List<ParticipacaoUsuarioResponse> participacoes = participacaoService.procurarPorPessoa(pessoa.getId()).stream()
                .map(ParticipacaoUsuarioResponse::from)
                .toList();
        return ResponseEntity.ok(participacoes);
    }

    private Pessoa pessoaAutenticada(JwtAuthenticationToken authentication) {
        String email = authentication.getToken().getSubject();
        return pessoaService.procurarPorEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado nao encontrado."));
    }
}
