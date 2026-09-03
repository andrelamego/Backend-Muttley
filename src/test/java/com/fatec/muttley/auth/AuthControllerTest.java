package com.fatec.muttley.auth;

import com.fatec.muttley.pessoa.*;
import com.fatec.muttley.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock PessoaService pessoas;
    @Mock PasswordEncoder encoder;
    @Mock JwtService tokens;
    AuthController controller;
    @BeforeEach void setup() {
        controller=new AuthController(mock(JwtDecoder.class));
        ReflectionTestUtils.setField(controller,"pessoaService",pessoas);
        ReflectionTestUtils.setField(controller,"passwordEncoder",encoder);
        ReflectionTestUtils.setField(controller,"jwtService",tokens);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void RN_AUT_04_05_primeiroAdministradorEDemaisUsuarios(boolean existeAdmin) {
        Pessoa p=pessoa(1); var dto=dadosPessoa(null,"senha");
        when(pessoas.existeAdmin()).thenReturn(existeAdmin);
        when(pessoas.salvarOuAtualizar(dto)).thenReturn(p); when(pessoas.salvar(p)).thenReturn(p);
        var response=controller.cadastrarUsuario(dto);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(((AuthController.UsuarioResponse)response.getBody()).role()).isEqualTo(existeAdmin?Role.USER:Role.ADMIN);
        verify(pessoas).salvar(p);
    }
    @Test void RN_AUT_01_emailDuplicadoRetorna409SemSalvar() {
        var dto=dadosPessoa(null,"senha"); when(pessoas.existePorEmail(dto.email())).thenReturn(true);
        assertThat(controller.cadastrarUsuario(dto).getStatusCode().value()).isEqualTo(409);
        verify(pessoas,never()).salvarOuAtualizar(any()); verify(pessoas,never()).salvar(any());
    }
    @Test void RN_AUT_06_emailDesconhecidoRetorna401SemToken() {
        assertThat(controller.validarCredenciais(new AuthController.LoginRequest("teste@example.invalid","senha")).getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(encoder,tokens);
    }
    @Test void RN_AUT_06_senhaErradaRetorna401SemToken() {
        Pessoa p=pessoa(1); p.setSenha("hash"); when(pessoas.procurarPorEmail(p.getEmail())).thenReturn(Optional.of(p));
        assertThat(controller.validarCredenciais(new AuthController.LoginRequest(p.getEmail(),"errada")).getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(tokens);
    }
    @Test void RF_AUT_02_03_loginValidoRetornaBearerEDadosSemSenha() {
        Pessoa p=pessoa(1); p.setSenha("hash"); when(pessoas.procurarPorEmail(p.getEmail())).thenReturn(Optional.of(p));
        when(encoder.matches("senha","hash")).thenReturn(true);
        when(tokens.gerarToken(p)).thenReturn("token-assinado"); when(tokens.getExpirationSeconds()).thenReturn(7200L);
        var response=controller.validarCredenciais(new AuthController.LoginRequest(p.getEmail(),"senha"));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        AuthController.LoginResponse body=(AuthController.LoginResponse)response.getBody();
        assertThat(body.accessToken()).isEqualTo("token-assinado"); assertThat(body.tokenType()).isEqualTo("Bearer");
        assertThat(body.expiresIn()).isEqualTo(7200); assertThat(body.usuario().id()).isEqualTo(1L);
    }
    @Test void RN_AUT_09_completaCadastroDaPessoaEncontradaPeloEmail() {
        Pessoa p=pessoa(1); var dto=dadosPessoa(99L,"senha");
        when(pessoas.procurarPorEmail(dto.email())).thenReturn(Optional.of(p));
        when(pessoas.salvarOuAtualizar(dto.withId(1L))).thenReturn(p);
        assertThat(controller.completarCadastro(dto).getStatusCode().value()).isEqualTo(200);
        verify(pessoas).salvarOuAtualizar(dto.withId(1L));
    }
    @Test void RN_AUT_10_cadastroCompletoRetorna409SemAlteracao() {
        Pessoa p=pessoa(1); p.setSenha("hash"); var dto=dadosPessoa(null,"senha");
        when(pessoas.procurarPorEmail(dto.email())).thenReturn(Optional.of(p));
        assertThat(controller.completarCadastro(dto).getStatusCode().value()).isEqualTo(409);
        verify(pessoas,never()).salvarOuAtualizar(any());
    }
    @Test void completarCadastroInexistenteRetorna404() {
        assertThat(controller.completarCadastro(dadosPessoa(null,"senha")).getStatusCode().value()).isEqualTo(404);
        verify(pessoas,never()).salvarOuAtualizar(any());
    }
}
