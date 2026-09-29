package com.fatec.muttley.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fatec.muttley.MuttleyApplication;
import com.fatec.muttley.massa.MassaDadosService;
import com.fatec.muttley.pdf.PdfClient;
import com.fatec.muttley.qrcode.QrCodeClient;
import com.fatec.muttley.participacao.InscricaoPublicaRequest;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.mysql.MySQLContainer;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MuttleyApplication.class, MassaDadosIT.Config.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MassaDadosIT {
    @TempDir static Path arquivos;
    @TestConfiguration
    static class Config {
        @Bean @Primary
        Clock clockMassa() {
            return Clock.fixed(Instant.parse("2026-09-29T13:30:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
        @Bean @ServiceConnection
        JdbcDatabaseContainer<?> bancoMassa() {
            return "mariadb".equals(System.getProperty("muttley.test.database"))
                    ? new MariaDBContainer("mariadb:10.4.32").withDatabaseName("massa_test").withUsername("test").withPassword("test")
                    : new MySQLContainer("mysql:8.4").withDatabaseName("massa_test").withUsername("test").withPassword("test");
        }
        @Bean
        MassaDadosService massaTeste(JdbcTemplate jdbc, PasswordEncoder encoder, Clock clock) {
            return new MassaDadosService(jdbc, encoder, clock, arquivos.toString());
        }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MassaDadosService massa;
    @Autowired TransactionTemplate transacoes;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    @Autowired Validator validator;
    @MockitoBean(name = "kafkaTemplate") KafkaTemplate<?, ?> kafka;
    @MockitoBean PdfClient pdf;
    @MockitoBean QrCodeClient qr;

    @BeforeEach
    void limparBancoDescartavel() {
        transacoes.executeWithoutResult(transacao -> {
            for (String tabela : List.of("certificado", "medalha", "participacao", "evento", "disciplina", "patrocinador",
                    "local", "endereco", "aluno", "professor", "palestrante", "organizador", "colaborador", "pessoa")) {
                jdbc.update("DELETE FROM " + tabela);
            }
            jdbc.update("UPDATE sequencia_inscricao SET valor=0 WHERE id=1");
        });
    }

    @Test
    void massaCompletaRespeitaVagasPresencaCertificadosEIdentidades() throws Exception {
        assertThat(massa.carregar()).isTrue();
        assertThat(contar("pessoa")).isEqualTo(72);
        assertThat(contar("evento")).isEqualTo(36);
        assertThat(contar("aluno")).isEqualTo(48);
        assertThat(contar("professor")).isEqualTo(8);
        assertThat(contar("palestrante")).isEqualTo(6);
        assertThat(contar("organizador")).isEqualTo(5);
        assertThat(contar("colaborador")).isEqualTo(4);
        assertThat(contar("endereco")).isEqualTo(6);
        assertThat(contar("local")).isEqualTo(7);
        assertThat(contar("disciplina")).isEqualTo(12);
        assertThat(contar("patrocinador")).isEqualTo(8);
        assertThat(contar("participacao")).isGreaterThan(500);
        assertThat(contar("certificado")).isEqualTo(147);
        assertThat(contar("medalha")).isEqualTo(209);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT p.id_evento FROM participacao p "
                + "JOIN evento e ON e.id_evento=p.id_evento JOIN local l ON l.id_local=e.id_local "
                + "GROUP BY p.id_evento,l.capacidade HAVING COUNT(*)>l.capacidade) cheios", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM certificado c JOIN participacao p "
                + "ON p.id_participacao=c.id_participacao JOIN evento e ON e.id_evento=p.id_evento "
                + "WHERE p.presente=0 OR e.status<>'FINALIZADO' OR c.data_emissao>DATE('2026-09-29')", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evento WHERE horario_inicio>=horario_fim", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT valor FROM sequencia_inscricao WHERE id=1", Long.class))
                .isEqualTo(contar("participacao"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao WHERE id_evento=40002", Long.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao WHERE id_evento=40003", Long.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao WHERE id_pessoa IN (10071,10072)", Long.class)).isZero();
        for (String cpf : jdbc.queryForList("SELECT cpf FROM pessoa WHERE cpf IS NOT NULL", String.class)) {
            assertThat(validator.validateValue(InscricaoPublicaRequest.class, "cpf", cpf)).isEmpty();
        }
        assertThat(encoder.matches("Muttley-Teste-2026!",
                jdbc.queryForObject("SELECT senha FROM pessoa WHERE id_pessoa=10001", String.class))).isTrue();
        String assinatura = jdbc.queryForObject("SELECT caminho_assinatura_visual FROM certificado LIMIT 1", String.class);
        assertThat(ImageIO.read(Path.of(assinatura).toFile())).isNotNull();
        verifyNoInteractions(kafka, pdf, qr);
    }

    @Test
    void reinicioMantemAlteracoesEAdministradorDoBootstrap() throws Exception {
        jdbc.update("INSERT INTO pessoa (id_pessoa,nome,email,senha,role) "
                + "VALUES (1,'Administrador local','admin@example.test','hash-original','ADMIN')");
        massa.carregar();
        jdbc.update("UPDATE evento SET tema='Alterado manualmente' WHERE id_evento=40001");
        assertThat(massa.carregar()).isFalse();
        assertThat(contar("pessoa")).isEqualTo(73);
        assertThat(jdbc.queryForObject("SELECT senha FROM pessoa WHERE id_pessoa=1", String.class)).isEqualTo("hash-original");
        assertThat(jdbc.queryForObject("SELECT tema FROM evento WHERE id_evento=40001", String.class)).isEqualTo("Alterado manualmente");
    }

    @Test
    void baseJaUtilizadaNaoEAlterada() {
        jdbc.update("INSERT INTO pessoa (nome,email,role) VALUES ('Pessoa existente','existente@example.test','USER')");
        assertThatThrownBy(() -> massa.carregar()).isInstanceOf(IllegalStateException.class);
        assertThat(contar("pessoa")).isEqualTo(1);
        assertThat(contar("evento")).isZero();
    }

    @Test
    void convitesAssinaturaELoginSaoUtilizaveisPelaApi() throws Exception {
        massa.carregar();
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"ana.massa@example.test\",\"senha\":\"Muttley-Teste-2026!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("usuario.role").value("USER"));
        mvc.perform(get("/api/pessoa/dados-cadastro/muttley-massa-convite-valido-2026"))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value("convite.valido@example.test"));
        mvc.perform(get("/api/pessoa/dados-cadastro/muttley-massa-convite-expirado-2026"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/pessoa/dados-cadastro/muttley-massa-convite-consumido-2026"))
                .andExpect(status().isNotFound());
        String codigo = jdbc.queryForObject("SELECT codigo_validacao FROM certificado ORDER BY id_certificado LIMIT 1", String.class);
        mvc.perform(get("/api/certificados/{codigo}", codigo)).andExpect(status().isOk());
        mvc.perform(get("/{id}/assinatura-visual", 60001)).andExpect(status().isOk());
        when(pdf.gerarPdf(anyString())).thenReturn("%PDF-fixture".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/certificados/{codigo}/download", codigo)).andExpect(status().isOk());
        String cpf = jdbc.queryForObject("SELECT cpf FROM pessoa WHERE id_pessoa=10067", String.class);
        String cadastro = new ObjectMapper().writeValueAsString(Map.of("nome", "Cadastro com convite válido",
                "email", "convite.valido@example.test", "cpf", cpf, "telefone", "11999999999", "senha", "Senha-nova-123!"));
        mvc.perform(put("/api/auth/register").param("token", "muttley-massa-convite-valido-2026")
                .contentType("application/json").content(cadastro)).andExpect(status().isOk());
        mvc.perform(get("/api/pessoa/dados-cadastro/muttley-massa-convite-valido-2026"))
                .andExpect(status().isNotFound());
    }

    @Test
    void horariosPermanecemValidosPertoDaViradaDoDia() throws Exception {
        var madrugada = new MassaDadosService(jdbc, encoder,
                Clock.fixed(Instant.parse("2026-09-30T02:58:00Z"), ZoneId.of("America/Sao_Paulo")), arquivos.toString());
        transacoes.executeWithoutResult(transacao -> {
            try { madrugada.carregar(); }
            catch (IOException exception) { throw new IllegalStateException(exception); }
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evento WHERE horario_inicio>=horario_fim", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT data FROM evento WHERE id_evento=40014", LocalDate.class))
                .isEqualTo(LocalDate.of(2026, 9, 30));
    }

    private long contar(String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class);
    }
}
