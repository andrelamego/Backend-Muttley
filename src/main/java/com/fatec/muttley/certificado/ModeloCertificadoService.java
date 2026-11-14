package com.fatec.muttley.certificado;

import com.fatec.muttley.arquivo.ImagemBase64;
import java.util.Locale;
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

    static String mimeDaLogo(String base64) {
        return ImagemBase64.mime(base64, LOGO_MAX_BYTES, "A logo");
    }
}
