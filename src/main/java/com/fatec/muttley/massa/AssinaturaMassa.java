package com.fatec.muttley.massa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.ClassPathResource;

/** Imagem identificada como fictícia para exercitar o upload e a composição do PDF. */
final class AssinaturaMassa {
    private AssinaturaMassa() {}

    static Path criar(String diretorio) throws IOException {
        Path arquivo = Path.of(diretorio).toAbsolutePath().normalize().resolve("massa/assinatura-ficticia.png");
        Files.createDirectories(arquivo.getParent());
        if (!Files.exists(arquivo)) {
            // Copiar o PNG evita exigir fontes ou ambiente gráfico no container da API.
            try (var imagem = new ClassPathResource("massa/assinatura-ficticia.png").getInputStream()) {
                Files.copy(imagem, arquivo);
            }
        }
        return arquivo;
    }
}
