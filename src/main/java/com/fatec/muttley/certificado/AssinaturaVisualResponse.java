package com.fatec.muttley.certificado;

import jakarta.persistence.EntityNotFoundException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class AssinaturaVisualResponse {
    private AssinaturaVisualResponse() {
    }

    static ResponseEntity<Resource> carregar(String caminho) {
        if (caminho == null || caminho.isBlank()) {
            throw ausente();
        }

        try {
            Path arquivo = Path.of(caminho);
            if (!Files.isRegularFile(arquivo) || !Files.isReadable(arquivo)) {
                throw ausente();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(AssinaturaStorage.mime(arquivo)))
                    .body(new FileSystemResource(arquivo));
        } catch (InvalidPathException exception) {
            throw ausente();
        }
    }

    private static EntityNotFoundException ausente() {
        return new EntityNotFoundException("Assinatura não encontrada.");
    }
}
