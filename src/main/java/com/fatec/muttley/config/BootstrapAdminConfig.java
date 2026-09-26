package com.fatec.muttley.config;

import com.fatec.muttley.pessoa.Pessoa;
import com.fatec.muttley.pessoa.PessoaRepository;
import com.fatec.muttley.pessoa.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
public class BootstrapAdminConfig {
    @Bean
    @ConditionalOnProperty(name = "muttley.bootstrap.admin.enabled", havingValue = "true")
    CommandLineRunner criarAdministradorInicial(
            PessoaRepository pessoas,
            PasswordEncoder encoder,
            @Value("${muttley.bootstrap.admin.email:}") String email,
            @Value("${muttley.bootstrap.admin.password:}") String senha,
            @Value("${muttley.bootstrap.admin.name:Administrador}") String nome) {
        return args -> {
            if (pessoas.existsByRole(Role.ADMIN)) {
                return;
            }
            if (!StringUtils.hasText(email) || !email.contains("@")
                    || !StringUtils.hasText(senha) || senha.length() < 12) {
                throw new IllegalStateException("Configure email e senha (mínimo 12 caracteres) para o administrador inicial.");
            }
            if (pessoas.existsByEmail(email)) {
                throw new IllegalStateException("O email do administrador inicial já pertence a outra conta.");
            }
            Pessoa admin = new Pessoa();
            admin.setNome(nome);
            admin.setEmail(email);
            admin.setSenha(encoder.encode(senha));
            admin.setRole(Role.ADMIN);
            pessoas.save(admin);
        };
    }
}
