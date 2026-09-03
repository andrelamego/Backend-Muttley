package com.fatec.muttley.certificado;

import com.fatec.muttley.evento.EventoService;
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

@RestController
@RequestMapping("/api/admin/certificados")
public class CertificadoController {

    @Autowired
    private CertificadoService certificadoService;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private AssinaturaStorage assinaturaStorage;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listarDados() {
        return ResponseEntity.ok(Map.of(
                "certificados", certificadoService.procurarTodos(),
                "eventosAguardandoCertificado", eventoService.procurarEventosAguardandoEmissaoCertificado(),
                "ultimosCertificados", certificadoService.procurarUltimosEmitidos()
        ));
    }

    @PostMapping("/evento/{eventoId}/upload-assinatura")
    @Transactional
    public ResponseEntity<?> salvarAssinaturaPorEvento(
            @PathVariable Long eventoId,
            @RequestParam("file") MultipartFile file) {
        eventoService.procurarPorId(eventoId).orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));
        String caminhoFinal = assinaturaStorage.salvar(file);
        certificadoService.atualizarAssinaturaPorEvento(eventoId, caminhoFinal);

        return ResponseEntity.ok(Map.of("mensagem", "Assinatura vinculada a todos os certificados do evento!"));
    }

    @PostMapping("/{id}/upload-assinatura")
    @Transactional
    public ResponseEntity<?> salvarAssinaturaNaPasta(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        certificadoService.procurarPorId(id).orElseThrow(() -> new EntityNotFoundException("Certificado não encontrado."));
        String caminhoFinal = assinaturaStorage.salvar(file);

        certificadoService.atualizarCaminhoAssinatura(id, caminhoFinal);

        return ResponseEntity.ok(Map.of("mensagem", "Assinatura vinculada com sucesso!"));
    }

    @GetMapping("/{id}/assinatura-visual")
    public ResponseEntity<Resource> exibirImagemDaPasta(@PathVariable Long id) {
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
