package com.fatec.muttley.certificado;

import com.fatec.muttley.evento.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Administração - Certificados", description = "Gestão administrativa de certificados emitidos e upload de assinaturas")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/certificados")
public class CertificadoController {

    @Autowired
    private CertificadoService certificadoService;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private AssinaturaStorage assinaturaStorage;

    @Operation(summary = "Listar dados gerais de certificados",
            description = "Retorna todos os certificados, eventos aguardando emissão e os últimos certificados emitidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados recuperados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer papel ADMIN)")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarDados() {
        return ResponseEntity.ok(Map.of(
                "certificados", certificadoService.procurarTodos(),
                "eventosAguardandoCertificado", eventoService.procurarEventosAguardandoEmissaoCertificado(),
                "ultimosCertificados", certificadoService.procurarUltimosEmitidos()
        ));
    }

    @Operation(summary = "Upload de assinatura em lote por evento",
            description = "Salva a imagem da assinatura e a vincula a todos os certificados gerados para o evento especificado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura vinculada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @PostMapping(value = "/evento/{eventoId}/upload-assinatura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> salvarAssinaturaPorEvento(
            @Parameter(description = "ID do evento")
            @PathVariable Long eventoId,
            @Parameter(description = "Arquivo de imagem da assinatura")
            @RequestParam("file") MultipartFile file) {
        eventoService.procurarPorId(eventoId).orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));
        String caminhoFinal = assinaturaStorage.salvar(file);
        certificadoService.atualizarAssinaturaPorEvento(eventoId, caminhoFinal);

        return ResponseEntity.ok(Map.of("mensagem", "Assinatura vinculada a todos os certificados do evento!"));
    }

    @Operation(summary = "Upload de assinatura individual para certificado",
            description = "Salva a imagem da assinatura e a vincula a um certificado específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura vinculada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Certificado não encontrado")
    })
    @PostMapping(value = "/{id}/upload-assinatura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> salvarAssinaturaNaPasta(
            @Parameter(description = "ID do certificado")
            @PathVariable Long id,
            @Parameter(description = "Arquivo de imagem da assinatura")
            @RequestParam("file") MultipartFile file) {
        certificadoService.procurarPorId(id).orElseThrow(() -> new EntityNotFoundException("Certificado não encontrado."));
        String caminhoFinal = assinaturaStorage.salvar(file);

        certificadoService.atualizarCaminhoAssinatura(id, caminhoFinal);

        return ResponseEntity.ok(Map.of("mensagem", "Assinatura vinculada com sucesso!"));
    }

    @Operation(summary = "Exibir imagem da assinatura visual",
            description = "Retorna o arquivo de imagem da assinatura associada ao certificado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Certificado ou arquivo não encontrado")
    })
    @GetMapping("/{id}/assinatura-visual")
    public ResponseEntity<Resource> exibirImagemDaPasta(
            @Parameter(description = "ID do certificado")
            @PathVariable Long id) {
        try {
            Certificado certificado = certificadoService.procurarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Certificado não encontrado"));

            String caminhoString = certificado.getCaminhoAssinaturaVisual();
            if (caminhoString == null || caminhoString.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path caminhoArquivo = Paths.get(caminhoString);
            Resource recurso = new UrlResource(caminhoArquivo.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(AssinaturaStorage.mime(caminhoArquivo)))
                        .body(recurso);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
