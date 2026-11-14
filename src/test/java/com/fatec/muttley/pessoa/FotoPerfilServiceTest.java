package com.fatec.muttley.pessoa;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FotoPerfilServiceTest {
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    @Mock FotoPerfilRepository repository;
    FotoPerfilService service;

    @BeforeEach void setup() {
        service = new FotoPerfilService(repository, Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test void salvaFotoValidaComTipoDescobertoPeloConteudo() {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        FotoPerfil foto = service.salvar(7L, " " + PNG + " ");
        assertThat(foto.getPessoaId()).isEqualTo(7L);
        assertThat(foto.getMime()).isEqualTo("image/png");
        assertThat(foto.getImagemBase64()).isEqualTo(PNG);
        assertThat(foto.getAtualizadaEm()).isEqualTo(Instant.parse("2026-11-14T12:00:00Z"));
    }

    @Test void recusaArquivoQueNaoEImagemEFotoVazia() {
        String svg = Base64.getEncoder().encodeToString("<svg onload=alert(1)>".getBytes());
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.salvar(7L, svg)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.salvar(7L, " ")).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test void recusaFotoGrandeDemais() {
        String grande = Base64.getEncoder().encodeToString(new byte[FotoPerfilService.FOTO_MAX_BYTES + 1]);
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.salvar(7L, grande)).hasMessageContaining("512 KB");
    }
}
