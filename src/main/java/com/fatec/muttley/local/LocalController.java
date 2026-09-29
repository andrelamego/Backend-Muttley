package com.fatec.muttley.local;

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

@Tag(name = "Administração - Locais", description = "Gestão dos locais físicos ou salas onde os eventos acontecem")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/locais")
@RequiredArgsConstructor
public class LocalController {

    private final LocalService localService;

    private final LocalMapper localMapper;

    @Operation(summary = "Listar todos os locais")
    @GetMapping
    public ResponseEntity<List<Local>> listarTodos() {
        return ResponseEntity.ok(localService.procurarTodos());
    }

    @Operation(summary = "Buscar local por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Local encontrado"),
            @ApiResponse(responseCode = "404", description = "Local não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AtualizacaoLocal> buscarPorId(
            @Parameter(description = "ID do local") @PathVariable Long id) {
        Local local = localService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Local não encontrado."));
        return ResponseEntity.ok(localMapper.toAtualizacaoDto(local));
    }

    @Operation(summary = "Criar novo local")
    @ApiResponse(responseCode = "201", description = "Local criado com sucesso")
    @PostMapping
    public ResponseEntity<Map<String, String>> criar(@RequestBody @Valid AtualizacaoLocal dto) {
        Local localSalvo = localService.salvarOuAtualizar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Local '" + localSalvo.getNome() + "' criado com sucesso!"));
    }

    @Operation(summary = "Atualizar local existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Local atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Local não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> atualizar(
            @Parameter(description = "ID do local") @PathVariable Long id,
            @RequestBody @Valid AtualizacaoLocal dto) {
        localService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Local não encontrado."));
        dto = dto.withId(id);
        Local localSalvo = localService.salvarOuAtualizar(dto);
        return ResponseEntity.ok(Map.of("message", "Local '" + localSalvo.getNome() + "' atualizado com sucesso!"));
    }

    @Operation(summary = "Excluir local")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Local excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Local não encontrado")
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deletar(
            @Parameter(description = "ID do local") @PathVariable Long id) {
        localService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Local não encontrado."));
        localService.apagarPorId(id);
        return ResponseEntity.ok(Map.of("message", "Local " + id + " foi apagado!"));
    }
}