package com.fatec.muttley.certificado;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Administração - Modelo do certificado", description = "Aparência dos certificados emitidos")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/certificados/modelo")
@RequiredArgsConstructor
public class ModeloCertificadoController {
    private static final String CODIGO_EXEMPLO = "8f166ceb-d791-3838-b0bc-faedc6c8b90c";

    private final ModeloCertificadoService modeloService;

    private final CertificadoHtml certificadoHtml;

    @Operation(summary = "Consultar o modelo do certificado")
    @GetMapping
    public ResponseEntity<ModeloCertificadoDados> consultar() {
        return ResponseEntity.ok(ModeloCertificadoDados.de(modeloService.atual()));
    }

    @Operation(summary = "Salvar o modelo do certificado",
            description = "Vale para os certificados gerados a partir de agora, inclusive os já emitidos ao serem baixados de novo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modelo salvo"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou logo que não é PNG/JPG")
    })
    @PutMapping
    public ResponseEntity<ModeloCertificadoDados> salvar(@RequestBody @Valid ModeloCertificadoDados dados) {
        return ResponseEntity.ok(ModeloCertificadoDados.de(modeloService.salvar(dados)));
    }

    @Operation(summary = "Prévia do certificado",
            description = "Devolve o HTML do certificado com dados de exemplo, sem salvar o modelo.")
    @PostMapping(value = "/previa", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previa(@RequestBody @Valid ModeloCertificadoDados dados) {
        var exemplo = new CertificadoHtml.Dados(
                "Ana Souza",
                "Participante",
                "Semana de Tecnologia da Fatec",
                LocalDate.now(),
                "3 horas",
                LocalDate.now(),
                CODIGO_EXEMPLO,
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .replacePath("/certificados/" + CODIGO_EXEMPLO).replaceQuery(null).build().toUriString(),
                null,
                null);
        String html = certificadoHtml.renderizar(certificadoHtml.variaveis(modeloService.rascunho(dados), exemplo));
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}
