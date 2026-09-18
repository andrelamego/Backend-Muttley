package com.fatec.muttley.certificado;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssinaturaStorage {
    private final Path diretorio;
    public AssinaturaStorage(@Value("${app.upload.assinaturas:uploads/assinaturas}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public String salvar(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalida();
        String nome = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean png = nome.endsWith(".png");
        boolean jpg = nome.endsWith(".jpg") || nome.endsWith(".jpeg");
        String esperado = png ? "image/png" : "image/jpeg";
        if ((!png && !jpg) || !esperado.equals(file.getContentType())) throw invalida();
        try {
            byte[] bytes = file.getBytes();
            // Confere o conteúdo, não apenas o nome/MIME informado pelo cliente.
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalida();
                var reader = readers.next();
                try {
                    String formato = reader.getFormatName();
                    if (!(png ? formato.equalsIgnoreCase("png") : formato.equalsIgnoreCase("JPEG"))) throw invalida();
                    reader.setInput(input);
                    if (reader.read(0) == null) throw invalida();
                } finally { reader.dispose(); }
            }
            Files.createDirectories(diretorio);
            Path arquivo = diretorio.resolve(UUID.randomUUID() + (png ? ".png" : ".jpg"));
            Files.write(arquivo, bytes, StandardOpenOption.CREATE_NEW);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try { Files.deleteIfExists(arquivo); }
                            catch (IOException e) { LoggerFactory.getLogger(AssinaturaStorage.class).error("Falha ao remover assinatura após rollback", e); }
                        }
                    }
                });
            }
            return arquivo.toString();
        } catch (IIOException e) {
            throw invalida();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao armazenar assinatura.", e);
        }
    }

    public static String mime(Path path) {
        return path.toString().toLowerCase(Locale.ROOT).matches(".*\\.jpe?g$") ? "image/jpeg" : "image/png";
    }

    private ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma assinatura JPG ou PNG válida.");
    }
}
