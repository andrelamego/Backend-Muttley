package com.fatec.muttley.security;

import com.fatec.muttley.pessoa.Role;
import jakarta.servlet.Filter;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static com.fatec.muttley.support.Cenarios.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Teste de fronteira HTTP da cadeia REAL de seguranca; sem Boot, banco ou Kafka. */
class AutorizacaoHttpTest {
    static AnnotationConfigWebApplicationContext context;
    static MockMvc mvc;
    @Configuration @EnableWebMvc @EnableWebSecurity @Import(SecurityConfig.class)
    static class Config {
        @Bean Endpoints endpoints() { return new Endpoints(); }
    }
    @RestController static class Endpoints {
        @RequestMapping("/api/**") String recurso() { return "ok"; }
    }
    @BeforeAll static void iniciar() {
        context=new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("teste",Map.of(
                "muttley.jwt.secret","chave-exclusiva-de-teste-com-mais-de-32-bytes")));
        context.register(Config.class); context.refresh();
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build();
    }
    @AfterAll static void fechar() { if(context!=null)context.close(); }
    private String token(Role role) {
        var p=pessoa(1); p.setRole(role);
        return new JwtService(context.getBean(JwtEncoder.class),Duration.ofHours(2)).gerarToken(p);
    }
    @ParameterizedTest @ValueSource(strings={"/api/eventos","/api/eventos/10","/api/inicio","/api/certificados/codigo","/api/certificados/codigo/download"})
    void consultasPublicasDispensamAutenticacao(String path) throws Exception { mvc.perform(get(path)).andExpect(status().isOk()); }
    @ParameterizedTest @ValueSource(strings={"/api/auth/login","/api/auth/register","/api/eventos/10/inscricoes","/api/eventos/10/confirmar-presenca/52998224725"})
    void operacoesPublicasDispensamAutenticacao(String path) throws Exception { mvc.perform(post(path)).andExpect(status().isOk()); }
    @ParameterizedTest @ValueSource(strings={"/api/admin/eventos","/api/me","/api/me/certificados","/api/me/participacoes","/api/me/medalhas","/api/participacoes"})
    void rotasProtegidasSemTokenRetornam401(String path) throws Exception { mvc.perform(get(path)).andExpect(status().isUnauthorized()); }
    @Test void userNaoAcessaAdministracao() throws Exception {
        mvc.perform(get("/api/admin/eventos").header("Authorization","Bearer "+token(Role.USER))).andExpect(status().isForbidden());
    }
    @Test void adminAcessaAdministracao() throws Exception {
        mvc.perform(get("/api/admin/eventos").header("Authorization","Bearer "+token(Role.ADMIN))).andExpect(status().isOk());
    }
    @Test void usuarioAutenticadoAcessaAreaPessoal() throws Exception {
        mvc.perform(get("/api/me").header("Authorization","Bearer "+token(Role.USER))).andExpect(status().isOk());
    }
    @Test void tokenMalformadoRetorna401() throws Exception {
        mvc.perform(get("/api/me").header("Authorization","Bearer invalido")).andExpect(status().isUnauthorized());
    }
}
