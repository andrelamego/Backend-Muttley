package com.fatec.muttley.disciplina;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Administração - Disciplinas", description = "Gestão de disciplinas acadêmicas vinculadas aos eventos")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/disciplinas")
public class DisciplinaController {

    @Autowired
    private DisciplinaService disciplinaService;

    @Autowired
    private DisciplinaMapper disciplinaMapper;

    @Operation(summary = "Listar todas as disciplinas")
    @GetMapping
    public ResponseEntity<List<Disciplina>> listarTodas() {
        return ResponseEntity.ok(disciplinaService.procurarTodas());
    }

    @Operation(summary = "Buscar disciplina por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disciplina encontrada"),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AtualizacaoDisciplina> buscarPorId(
            @Parameter(description = "ID da disciplina") @PathVariable Long id) {
        Disciplina disciplina = disciplinaService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Disciplina não encontrada."));
        return ResponseEntity.ok(disciplinaMapper.toAtualizacaoDto(disciplina));
    }

    @Operation(summary = "Criar nova disciplina")
    @ApiResponse(responseCode = "201", description = "Disciplina criada com sucesso")
    @PostMapping
    public ResponseEntity<Map<String, String>> criar(@RequestBody @Valid AtualizacaoDisciplina dto) {
        Disciplina disciplinaSalva = disciplinaService.salvarOuAtualizar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Disciplina '" + disciplinaSalva.getNome() + "' criada com sucesso."));
    }

    @Operation(summary = "Atualizar disciplina existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disciplina atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> atualizar(
            @Parameter(description = "ID da disciplina") @PathVariable Long id,
            @RequestBody @Valid AtualizacaoDisciplina dto) {
        disciplinaService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Disciplina não encontrada."));
        dto = dto.withId(id);
        Disciplina disciplinaSalva = disciplinaService.salvarOuAtualizar(dto);
        return ResponseEntity.ok(Map.of("message", "Disciplina '" + disciplinaSalva.getNome() + "' atualizada com sucesso."));
    }

    @Operation(summary = "Excluir disciplina")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disciplina excluída com sucesso"),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada")
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deletar(
            @Parameter(description = "ID da disciplina") @PathVariable Long id) {
        disciplinaService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Disciplina não encontrada."));
        disciplinaService.apagarPorId(id);
        return ResponseEntity.ok(Map.of("message", "Disciplina " + id + " deletada com sucesso."));
    }
}