package com.fatec.muttley.medalha;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Administração - Medalhas", description = "Gestão de medalhas e conquistas do sistema de gamificação")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/medalhas")
@RequiredArgsConstructor
public class MedalhaController {

    private final MedalhaService medalhaService;

    private final MedalhaMapper medalhaMapper;

    @Operation(summary = "Listar todas as medalhas")
    @GetMapping
    public ResponseEntity<List<Medalha>> listarTodos() {
        return ResponseEntity.ok(medalhaService.procurarTodos());
    }

    @Operation(summary = "Buscar medalha por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medalha encontrada"),
            @ApiResponse(responseCode = "404", description = "Medalha não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AtualizacaoMedalha> buscarPorId(
            @Parameter(description = "ID da medalha") @PathVariable Long id) {
        Medalha medalha = medalhaService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Medalha não encontrada."));
        return ResponseEntity.ok(medalhaMapper.toAtualizacaoDto(medalha));
    }

    @Operation(summary = "Criar nova medalha")
    @ApiResponse(responseCode = "201", description = "Medalha criada com sucesso")
    @PostMapping
    public ResponseEntity<Map<String, String>> criar(@RequestBody @Valid AtualizacaoMedalha dto) {
        Medalha medalhaSalva = medalhaService.salvarOuAtualizar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Medalha '" + medalhaSalva.getNome() + "' criada com sucesso!"));
    }

    @Operation(summary = "Atualizar medalha existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medalha atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Medalha não encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> atualizar(
            @Parameter(description = "ID da medalha") @PathVariable Long id,
            @RequestBody @Valid AtualizacaoMedalha dto) {
        medalhaService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Medalha não encontrada."));
        dto = dto.withId(id);
        Medalha medalhaSalva = medalhaService.salvarOuAtualizar(dto);
        return ResponseEntity.ok(Map.of("message", "Medalha '" + medalhaSalva.getNome() + "' atualizada com sucesso!"));
    }

    @Operation(summary = "Excluir medalha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medalha excluída com sucesso"),
            @ApiResponse(responseCode = "404", description = "Medalha não encontrada")
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deletar(
            @Parameter(description = "ID da medalha") @PathVariable Long id) {
        medalhaService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Medalha não encontrada."));
        medalhaService.apagarPorId(id);
        return ResponseEntity.ok(Map.of("message", "Medalha " + id + " foi apagada!"));
    }
}