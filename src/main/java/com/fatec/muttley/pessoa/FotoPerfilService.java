package com.fatec.muttley.pessoa;

import com.fatec.muttley.arquivo.ImagemBase64;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FotoPerfilService {
    static final int FOTO_MAX_BYTES = 512 * 1024;

    private final FotoPerfilRepository repository;
    private final Clock clock;

    public Optional<FotoPerfil> daPessoa(Long pessoaId) {
        return repository.findById(pessoaId);
    }

    public boolean temFoto(Long pessoaId) {
        return repository.existsById(pessoaId);
    }

    @Transactional
    public FotoPerfil salvar(Long pessoaId, String base64) {
        if (base64 == null || base64.isBlank()) throw new IllegalArgumentException("Envie uma foto.");
        String limpo = base64.trim();
        FotoPerfil foto = repository.findById(pessoaId).orElseGet(FotoPerfil::new);
        foto.setPessoaId(pessoaId);
        foto.setMime(ImagemBase64.mime(limpo, FOTO_MAX_BYTES, "A foto"));
        foto.setImagemBase64(limpo);
        foto.setAtualizadaEm(Instant.now(clock));
        return repository.save(foto);
    }

    @Transactional
    public void remover(Long pessoaId) {
        repository.deleteById(pessoaId);
    }
}
