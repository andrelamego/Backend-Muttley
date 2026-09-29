package com.fatec.muttley;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Sistema", description = "Verificação de disponibilidade e integridade da API")
@RestController
@RequestMapping("/api/inicio")
public class InicioController {

    @Operation(summary = "Verificar status da API", description = "Retorna o status de integridade do serviço Muttley.")
    @ApiResponse(responseCode = "200", description = "API em operação normal")
    @GetMapping
    public ResponseEntity<Map<String, String>> carregarIndex() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "A API Muttley está online e a funcionar!"
        ));
    }
}