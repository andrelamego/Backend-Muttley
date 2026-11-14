package com.fatec.muttley.arquivo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Imagens recebidas em base64 (logo do certificado, foto de perfil): confere o conteúdo, não o nome. */
public final class ImagemBase64 {
    private ImagemBase64() {
    }

    /**
     * Devolve image/png ou image/jpeg; lança IllegalArgumentException se não for uma imagem PNG ou JPG
     * válida ou passar do tamanho. {@code nome} entra nas mensagens ("A logo", "A foto").
     */
    public static String mime(String base64, int maxBytes, String nome) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(nome + " enviada não é uma imagem válida.");
        }
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException(nome + " deve ter até " + (maxBytes / 1024) + " KB.");
        }
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var leitores = ImageIO.getImageReaders(input);
            if (!leitores.hasNext()) throw new IllegalArgumentException("Envie a imagem em PNG ou JPG.");
            var leitor = leitores.next();
            try {
                String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                leitor.setInput(input);
                if (leitor.read(0) == null) throw new IllegalArgumentException(nome + " enviada não é uma imagem válida.");
                if (formato.equals("png")) return "image/png";
                if (formato.equals("jpeg") || formato.equals("jpg")) return "image/jpeg";
                throw new IllegalArgumentException("Envie a imagem em PNG ou JPG.");
            } finally {
                leitor.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(nome + " enviada não é uma imagem válida.");
        }
    }
}
