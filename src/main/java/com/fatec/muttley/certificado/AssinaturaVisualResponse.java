package com.fatec.muttley.certificado;

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
            return ResponseEntity.notFound().build();
        }

        try {
            Path arquivo = Path.of(caminho);
            if (!Files.isRegularFile(arquivo) || !Files.isReadable(arquivo)) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(AssinaturaStorage.mime(arquivo)))
                    .body(new FileSystemResource(arquivo));
        } catch (InvalidPathException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
