package com.fatec.muttley.patrocinador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Administração - Patrocinadores", description = "Gestão de empresas e parceiros patrocinadores de eventos")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/patrocinadores")
@RequiredArgsConstructor
public class PatrocinadorController {

    private final PatrocinadorService patrocinadorService;

    private final PatrocinadorMapper patrocinadorMapper;

    @Operation(summary = "Listar todos os patrocinadores")
    @GetMapping
    public ResponseEntity<List<Patrocinador>> listarTodos() {
        return ResponseEntity.ok(patrocinadorService.procurarTodos());
    }

    @Operation(summary = "Buscar patrocinador por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patrocinador encontrado"),
            @ApiResponse(responseCode = "404", description = "Patrocinador não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AtualizacaoPatrocinador> buscarPorId(
            @Parameter(description = "ID do patrocinador") @PathVariable Long id) {
        Patrocinador patrocinador = patrocinadorService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Patrocinador não encontrado."));
        return ResponseEntity.ok(patrocinadorMapper.toAtualizacaoDto(patrocinador));
    }

    @Operation(summary = "Criar novo patrocinador")
    @ApiResponse(responseCode = "201", description = "Patrocinador criado com sucesso")
    @PostMapping
    public ResponseEntity<Map<String, String>> criar(@RequestBody @Valid AtualizacaoPatrocinador dto) {
        Patrocinador patrocinadorSalvo = patrocinadorService.salvarOuAtualizar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Patrocinador '" + patrocinadorSalvo.getNome() + "' criado com sucesso."));
    }

    @Operation(summary = "Atualizar patrocinador existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patrocinador atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Patrocinador não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> atualizar(
            @Parameter(description = "ID do patrocinador") @PathVariable Long id,
            @RequestBody @Valid AtualizacaoPatrocinador dto) {
        patrocinadorService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Patrocinador não encontrado."));
        dto = dto.withId(id);
        Patrocinador patrocinadorSalvo = patrocinadorService.salvarOuAtualizar(dto);
        return ResponseEntity.ok(Map.of("message", "Patrocinador '" + patrocinadorSalvo.getNome() + "' atualizado com sucesso."));
    }

    @Operation(summary = "Excluir patrocinador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patrocinador excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Patrocinador não encontrado")
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deletar(
            @Parameter(description = "ID do patrocinador") @PathVariable Long id) {
        patrocinadorService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Patrocinador não encontrado."));
        patrocinadorService.apagarPorId(id);
        return ResponseEntity.ok(Map.of("message", "Patrocinador " + id + " deletado com sucesso."));
    }
}