package com.fatec.muttley.massa;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MassaDadosConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MassaDadosConfig.class)
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
            .withBean(Clock.class, Clock::systemUTC);

    @Test
    void cargaExigeProfileEPropriedadeExplicitos() {
        runner.run(contexto -> assertThat(contexto).doesNotHaveBean(MassaDadosService.class));
        runner.withPropertyValues("muttley.massa.enabled=true")
                .run(contexto -> assertThat(contexto).doesNotHaveBean(MassaDadosService.class));
        runner.withInitializer(contexto -> contexto.getEnvironment().setActiveProfiles("massa"))
                .run(contexto -> assertThat(contexto).doesNotHaveBean(MassaDadosService.class));
        runner.withInitializer(contexto -> contexto.getEnvironment().setActiveProfiles("massa"))
                .withPropertyValues("muttley.massa.enabled=true")
                .run(contexto -> assertThat(contexto).hasSingleBean(MassaDadosService.class));
    }

    @Test
    void profileTestImpedeCargaMesmoComAtivacaoExplicita() {
        runner.withInitializer(contexto -> contexto.getEnvironment().setActiveProfiles("massa", "test"))
                .withPropertyValues("muttley.massa.enabled=true")
                .run(contexto -> assertThat(contexto).doesNotHaveBean(MassaDadosService.class));
    }
}
