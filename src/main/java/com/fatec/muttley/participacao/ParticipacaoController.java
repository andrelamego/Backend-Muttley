package com.fatec.muttley.participacao;

import com.fatec.muttley.email.EmailProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Participações", description = "Endpoints de consulta e gerenciamento de participações em eventos")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/participacoes")
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ParticipacaoController {
    @Autowired
    private ParticipacaoAcessoService acesso;

    @Autowired
    private ParticipacaoService participacaoService;

    @Autowired
    private ParticipacaoMapper participacaoMapper;

    @Autowired
    private EmailProducer emailProducer;

    @Operation(summary = "Listar participações permitidas",
            description = "Retorna as participações acessíveis ao usuário atual (todas para ADMIN ou próprias para USER).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    public ResponseEntity<List<ParticipacaoComEventoResponse>> listarTodos() {
        List<ParticipacaoComEventoResponse> participacoes = acesso.listar().stream()
                .map(ParticipacaoComEventoResponse::from)
                .toList();
        return ResponseEntity.ok(participacoes);
    }

    @Operation(summary = "Buscar participação por ID",
            description = "Retorna os dados detalhados da participação e seu evento associado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participação encontrada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido à participação de terceiros"),
            @ApiResponse(responseCode = "404", description = "Participação não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ParticipacaoComEventoResponse> buscarPorId(
            @Parameter(description = "ID da participação") @PathVariable Long id) {
        Participacao participacao = participacaoService.procurarPorIdComDados(id)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada."));
        acesso.validarParticipacao(participacao);
        return ResponseEntity.ok(ParticipacaoComEventoResponse.from(participacao));
    }

    @Operation(summary = "Criar nova participação",
            description = "Cadastra uma nova participação de uma pessoa em um evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participação criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido")
    })
    @PostMapping
    public ResponseEntity<Map<String, String>> criar(@RequestBody @Valid AtualizacaoParticipacao dto) {
        acesso.validarPessoa(dto.pessoaId());
        Participacao participacaoSalva = participacaoService.salvarOuAtualizar(dto.withId(null));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Participação '" + participacaoSalva.getInscricao() + "' criada com sucesso."));
    }

    @Operation(summary = "Atualizar participação existente",
            description = "Atualiza os dados de vínculo ou tipo de participação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participação alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido"),
            @ApiResponse(responseCode = "404", description = "Participação não encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> atualizar(
            @Parameter(description = "ID da participação") @PathVariable Long id,
            @RequestBody @Valid AtualizacaoParticipacao dto) {
        Participacao existente = participacaoService.procurarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada."));
        acesso.validarParticipacao(existente);
        acesso.validarPessoa(dto.pessoaId());
        dto = dto.withId(id);
        Participacao participacaoSalva = participacaoService.salvarOuAtualizar(dto);
        return ResponseEntity.ok(Map.of("message", "Participação '" + participacaoSalva.getInscricao() + "' alterada com sucesso."));
    }

    @Operation(summary = "Deletar participação",
            description = "Remove uma participação de evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participação deletada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido"),
            @ApiResponse(responseCode = "404", description = "Participação não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletar(
            @Parameter(description = "ID da participação") @PathVariable Long id) {
        Participacao existente = participacaoService.procurarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada."));
        acesso.validarParticipacao(existente);
        participacaoService.apagarPorId(id);
        return ResponseEntity.ok(Map.of("message", "Participação " + id + " deletada com sucesso."));
    }
}
