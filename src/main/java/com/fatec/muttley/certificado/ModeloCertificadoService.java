package com.fatec.muttley.certificado;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModeloCertificadoService {
    static final int LOGO_MAX_BYTES = 512 * 1024;

    private final ModeloCertificadoRepository repository;

    public ModeloCertificado atual() {
        return repository.findById(ModeloCertificado.ID_UNICO).orElseGet(ModeloCertificado::padrao);
    }

    @Transactional
    public ModeloCertificado salvar(ModeloCertificadoDados dados) {
        ModeloCertificado modelo = atual();
        aplicar(modelo, dados);
        return repository.save(modelo);
    }

    /** Monta um modelo sem salvar, para a prévia; a logo é validada do mesmo jeito. */
    public ModeloCertificado rascunho(ModeloCertificadoDados dados) {
        ModeloCertificado modelo = ModeloCertificado.padrao();
        aplicar(modelo, dados);
        return modelo;
    }

    private void aplicar(ModeloCertificado modelo, ModeloCertificadoDados dados) {
        modelo.setInstituicao(dados.instituicao().trim());
        modelo.setTitulo(dados.titulo().trim());
        modelo.setTexto(dados.texto().trim());
        modelo.setCargoAssinatura(dados.cargoAssinatura().trim());
        modelo.setCorPrincipal(dados.corPrincipal().toLowerCase(Locale.ROOT));
        modelo.setCorDestaque(dados.corDestaque().toLowerCase(Locale.ROOT));
        modelo.setLeiaute(dados.leiaute());
        if (dados.logoBase64() == null || dados.logoBase64().isBlank()) {
            modelo.setLogoBase64(null);
            modelo.setLogoMime(null);
        } else {
            String base64 = dados.logoBase64().trim();
            modelo.setLogoMime(mimeDaLogo(base64));
            modelo.setLogoBase64(base64);
        }
    }

    /** Confere o conteúdo da imagem (não o nome informado) e devolve image/png ou image/jpeg. */
    static String mimeDaLogo(String base64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("A logo enviada não é uma imagem válida.");
        }
        if (bytes.length > LOGO_MAX_BYTES) {
            throw new IllegalArgumentException("A logo deve ter até 512 KB.");
        }
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var leitores = ImageIO.getImageReaders(input);
            if (!leitores.hasNext()) throw new IllegalArgumentException("Envie a logo em PNG ou JPG.");
            var leitor = leitores.next();
            try {
                String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                leitor.setInput(input);
                if (leitor.read(0) == null) throw new IllegalArgumentException("A logo enviada não é uma imagem válida.");
                if (formato.equals("png")) return "image/png";
                if (formato.equals("jpeg") || formato.equals("jpg")) return "image/jpeg";
                throw new IllegalArgumentException("Envie a logo em PNG ou JPG.");
            } finally {
                leitor.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("A logo enviada não é uma imagem válida.");
        }
    }
}
