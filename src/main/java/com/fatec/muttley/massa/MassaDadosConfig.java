package com.fatec.muttley.massa;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("massa & !test")
@ConditionalOnProperty(name = "muttley.massa.enabled", havingValue = "true")
public class MassaDadosConfig {
    @Bean
    MassaDadosService massaDadosService(JdbcTemplate jdbc, PasswordEncoder encoder, Clock clock,
            @Value("${app.upload.assinaturas:uploads/assinaturas}") String assinaturas) {
        return new MassaDadosService(jdbc, encoder, clock, assinaturas);
    }

    @Bean
    @Order(100)
    CommandLineRunner carregarMassaDados(MassaDadosService massa) {
        return args -> massa.carregar();
    }
}
