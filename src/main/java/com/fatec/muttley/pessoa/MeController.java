package com.fatec.muttley.pessoa;

import com.fatec.muttley.certificado.CertificadoService;
import com.fatec.muttley.conquista.ConquistaService;
import com.fatec.muttley.participacao.ParticipacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Meu Perfil", description = "Endpoints de consulta exclusivos do usuário autenticado")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class MeController {

    private final PessoaService pessoaService;

    private final CertificadoService certificadoService;

    private final ConquistaService conquistaService;

    private final FotoPerfilService fotoPerfilService;

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
        return ResponseEntity.ok(PessoaMeResponse.from(pessoa, fotoPerfilService.temFoto(pessoa.getId())));
    }

    @Operation(summary = "Atualizar dados do usuário autenticado", description = "Altera nome e telefone da própria conta; email e CPF não mudam por aqui.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados atualizados"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado ou token inválido")
    })
    @PutMapping("/api/me")
    public ResponseEntity<PessoaMeResponse> atualizarUsuarioAutenticado(
            JwtAuthenticationToken authentication, @RequestBody @Valid AtualizacaoPerfil dados) {
        Pessoa pessoa = pessoaService.atualizarPerfil(pessoaAutenticada(authentication), dados);
        return ResponseEntity.ok(PessoaMeResponse.from(pessoa, fotoPerfilService.temFoto(pessoa.getId())));
    }

    @Operation(summary = "Alterar a senha do usuário autenticado", description = "Exige a senha atual.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada"),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado ou token inválido")
    })
    @PutMapping("/api/me/senha")
    public ResponseEntity<Void> alterarSenha(
            JwtAuthenticationToken authentication, @RequestBody @Valid AlteracaoSenha dados) {
        pessoaService.alterarSenha(pessoaAutenticada(authentication), dados);
        return ResponseEntity.noContent().build();
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

    public record FotoRequest(String imagemBase64) {
    }

    @Operation(summary = "Foto de perfil do usuário autenticado", description = "Devolve a imagem; 404 se não houver foto.")
    @GetMapping("/api/me/foto")
    public ResponseEntity<byte[]> foto(JwtAuthenticationToken authentication) {
        return fotoPerfilService.daPessoa(pessoaAutenticada(authentication).getId())
                .map(f -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(f.getMime()))
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .body(Base64.getDecoder().decode(f.getImagemBase64())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sem foto de perfil."));
    }

    @Operation(summary = "Trocar a foto de perfil", description = "PNG ou JPG em base64, até 512 KB.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Foto salva"),
            @ApiResponse(responseCode = "400", description = "Imagem inválida ou grande demais")
    })
    @PutMapping("/api/me/foto")
    public ResponseEntity<Void> trocarFoto(JwtAuthenticationToken authentication, @RequestBody FotoRequest dados) {
        fotoPerfilService.salvar(pessoaAutenticada(authentication).getId(), dados.imagemBase64());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remover a foto de perfil")
    @DeleteMapping("/api/me/foto")
    public ResponseEntity<Void> removerFoto(JwtAuthenticationToken authentication) {
        fotoPerfilService.remover(pessoaAutenticada(authentication).getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Medalhas do usuário autenticado",
            description = "Trilhas com nível e progresso, medalhas especiais conquistadas e a posição nas disputas do semestre.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medalhas calculadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado ou token inválido")
    })
    @GetMapping("/api/me/conquistas")
    public ResponseEntity<ConquistaService.ConquistasResponse> conquistas(JwtAuthenticationToken authentication) {
        return ResponseEntity.ok(conquistaService.daPessoa(pessoaAutenticada(authentication).getId()));
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
