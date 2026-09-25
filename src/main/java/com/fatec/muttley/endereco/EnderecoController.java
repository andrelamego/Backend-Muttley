package com.fatec.muttley.endereco;

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

@Tag(name = "Administração - Endereços", description = "Gestão de endereços de locais de eventos")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/enderecos")
@RequiredArgsConstructor
public class EnderecoController {

    private final EnderecoService enderecoService;

    private final EnderecoMapper enderecoMapper;

    @Operation(summary = "Listar todos os endereços")
    @GetMapping
    public ResponseEntity<List<Endereco>> listarTodos() {
        return ResponseEntity.ok(enderecoService.procurarTodos());
    }

    @Operation(summary = "Buscar endereço por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço encontrado"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AtualizacaoEndereco> buscarPorId(
            @Parameter(description = "ID do endereço") @PathVariable Long id) {
        Endereco endereco = enderecoService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Endereco não encontrado."));
        return ResponseEntity.ok(enderecoMapper.toAtualizacaoDto(endereco));
    }

    @Operation(summary = "Criar novo endereço")
    @ApiResponse(responseCode = "201", description = "Endereço criado com sucesso")
    @PostMapping
    public ResponseEntity<Map<String, String>> criar(@RequestBody @Valid AtualizacaoEndereco dto) {
        Endereco enderecoSalvo = enderecoService.salvarOuAtualizar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Endereço '" + enderecoSalvo.getLogradouro() + "' criado com sucesso!"));
    }

    @Operation(summary = "Atualizar endereço existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> atualizar(
            @Parameter(description = "ID do endereço") @PathVariable Long id,
            @RequestBody @Valid AtualizacaoEndereco dto) {
        enderecoService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Endereco não encontrado."));
        dto = dto.withId(id);
        Endereco enderecoSalvo = enderecoService.salvarOuAtualizar(dto);
        return ResponseEntity.ok(Map.of("message", "Endereço '" + enderecoSalvo.getLogradouro() + "' atualizado com sucesso!"));
    }

    @Operation(summary = "Excluir endereço")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado")
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deletar(
            @Parameter(description = "ID do endereço") @PathVariable Long id) {
        enderecoService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Endereco não encontrado."));
        enderecoService.apagarPorId(id);
        return ResponseEntity.ok(Map.of("message", "Endereço " + id + " foi apagado!"));
    }
}