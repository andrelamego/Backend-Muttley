package com.fatec.muttley.evento;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fatec.muttley.evento.enums.ModalidadeEventoEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventoAdminResponseTest {
    private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();

    @Test void camposDoEventoFicamNaRaizComOsPalestrantesAoLado() throws Exception {
        var evento = new AtualizacaoEvento(7L, "Tema", "Descrição", null, "09:00", "11:00",
                ModalidadeEventoEnum.PRESENCIAL, null, 1L, 2L, 3L);
        var resposta = new EventoAdminResponse(evento, List.of(new EventoAdminResponse.PalestranteResumo(5L, "Ana")));

        var arvore = json.readTree(json.writeValueAsString(resposta));

        // O formulário do front lê tema, localId etc. direto da raiz, como antes.
        assertThat(arvore.path("tema").asText()).isEqualTo("Tema");
        assertThat(arvore.path("localId").asLong()).isEqualTo(3L);
        assertThat(arvore.has("evento")).isFalse();
        assertThat(arvore.path("palestrantes").get(0).path("nome").asText()).isEqualTo("Ana");
    }
}
