package com.fatec.muttley.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FalhasController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @RestController
    static class FalhasController {
        @GetMapping("/falha-interna")
        void interna() {
            throw new RuntimeException("senha-do-banco-nao-deve-ser-exposta");
        }

        @GetMapping("/servico-indisponivel")
        void externo() {
            throw new RestClientException("detalhes internos do serviço PDF");
        }
    }

    @Test
    void erroInesperadoNaoRevelaDetalhesInternos() throws Exception {
        mvc.perform(get("/falha-interna"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.erro").value("Erro interno do servidor."))
                .andExpect(jsonPath("$.erros").isEmpty())
                .andExpect(jsonPath("$.caminho").value("/falha-interna"));
    }

    @Test
    void falhaDoServicoExternoRetorna503SemExporDetalhes() throws Exception {
        mvc.perform(get("/servico-indisponivel"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("SERVICO_INDISPONIVEL"))
                .andExpect(jsonPath("$.erro").value("Serviço externo indisponível. Tente novamente em alguns instantes."));
    }
}
