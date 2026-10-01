package com.fatec.muttley.auth;

import com.fatec.muttley.pessoa.*;
import com.fatec.muttley.email.EmailProducer;
import com.fatec.muttley.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock PessoaService pessoas;
    @Mock PasswordEncoder encoder;
    @Mock JwtService tokens;
    @Mock CadastroConviteService convites;
    @Mock EmailProducer emails;
    AuthController controller;
    @BeforeEach void setup() {
        controller=new AuthController(pessoas,encoder,tokens,mock(JwtDecoder.class),convites,emails);
    }
    @Test void cadastroPublicoCriaContaPendenteSemAdministrador() {
        Pessoa p=pessoa(1); var dto=new SolicitacaoCadastro(p.getNome(),p.getEmail());
        when(pessoas.salvar(any(Pessoa.class))).thenAnswer(call -> { Pessoa salvo=call.getArgument(0);salvo.setId(1L);return salvo; });
        when(convites.emitir(any(Pessoa.class))).thenReturn(Optional.of("convite"));
        var response=controller.cadastrarUsuario(dto);
        assertThat(response.getStatusCode().value()).isEqualTo(202);
        ArgumentCaptor<Pessoa> captor=ArgumentCaptor.forClass(Pessoa.class);
        verify(pessoas).salvar(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(captor.getValue().getSenha()).isNull();
        assertThat(captor.getValue().getCpf()).isNull();
        verify(convites).emitir(captor.getValue());
        verify(pessoas,never()).existeAdmin();
    }
    @Test void cadastroExistenteNaoRevelaContaNemEnviaConvite() {
        Pessoa p=pessoa(1); p.setSenha("hash");
        var dto=new SolicitacaoCadastro(p.getNome(),p.getEmail());
        when(pessoas.procurarPorEmail(dto.email())).thenReturn(Optional.of(p));
        assertThat(controller.cadastrarUsuario(dto).getStatusCode().value()).isEqualTo(202);
        verify(pessoas,never()).salvar(any());
        verifyNoInteractions(convites,emails);
    }
    @Test void RN_AUT_06_emailDesconhecidoRetorna401SemToken() {
        assertThatThrownBy(() -> controller.validarCredenciais(new AuthController.LoginRequest("teste@example.invalid","senha")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(erro -> assertThat(((ResponseStatusException) erro).getStatusCode().value()).isEqualTo(401));
        verifyNoInteractions(encoder,tokens);
    }
    @Test void RN_AUT_06_senhaErradaRetorna401SemToken() {
        Pessoa p=pessoa(1); p.setSenha("hash"); when(pessoas.procurarPorEmail(p.getEmail())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> controller.validarCredenciais(new AuthController.LoginRequest(p.getEmail(),"errada")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(erro -> assertThat(((ResponseStatusException) erro).getStatusCode().value()).isEqualTo(401));
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
    @Test void RN_AUT_09_completaCadastroSomenteComConvite() {
        Pessoa p=pessoa(1); var dto=dadosPessoa(99L,"senha");
        when(convites.concluir("convite",dto)).thenReturn(p);
        assertThat(controller.completarCadastro("convite",dto).getStatusCode().value()).isEqualTo(200);
        verify(convites).concluir("convite",dto);
        verify(pessoas,never()).procurarPorEmail(any());
    }
}
