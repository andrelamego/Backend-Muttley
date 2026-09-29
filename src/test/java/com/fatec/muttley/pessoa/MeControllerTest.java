package com.fatec.muttley.pessoa;

import com.fatec.muttley.certificado.CertificadoService;
import com.fatec.muttley.medalha.MedalhaService;
import com.fatec.muttley.participacao.ParticipacaoService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {
    @Mock PessoaService pessoas; @Mock CertificadoService certificados;
    @Mock MedalhaService medalhas; @Mock ParticipacaoService participacoes;
    @InjectMocks MeController controller;
    private JwtAuthenticationToken token() {
        return new JwtAuthenticationToken(Jwt.withTokenValue("teste").header("alg","HS256").subject("pessoa7@example.invalid").claim("userId",99L).build());
    }
    @Test void RF_AUT_05_RF_PAR_03_RF_CER_08_RF_MED_02_consultasUsamPessoaDoSubject() {
        when(pessoas.procurarPorEmail("pessoa7@example.invalid")).thenReturn(Optional.of(pessoa(7)));
        assertThat(controller.buscarUsuarioAutenticado(token()).getStatusCode().value()).isEqualTo(200);
        assertThat(controller.listarParticipacoes(token()).getBody()).isEmpty();
        assertThat(controller.listarCertificados(token()).getBody()).isEmpty();
        assertThat(controller.listarMedalhas(token()).getBody()).isEmpty();
        verify(participacoes).procurarPorPessoa(7L); verify(certificados).procurarPorPessoa(7L); verify(medalhas).procurarPorPessoa(7L);
        verifyNoMoreInteractions(participacoes,certificados,medalhas);
    }
    @Test void usuarioDoTokenInexistenteRetorna404() {
        assertThatThrownBy(() -> controller.listarCertificados(token())).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(certificados,medalhas,participacoes);
    }
}
