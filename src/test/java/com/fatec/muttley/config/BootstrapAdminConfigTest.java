package com.fatec.muttley.config;

import com.fatec.muttley.pessoa.Pessoa;
import com.fatec.muttley.pessoa.PessoaRepository;
import com.fatec.muttley.pessoa.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BootstrapAdminConfigTest {
    PessoaRepository pessoas = mock(PessoaRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    BootstrapAdminConfig config = new BootstrapAdminConfig();

    @Test void criaAdministradorSomenteComCredenciaisExplicitas() throws Exception {
        when(encoder.encode("senha-forte-123")).thenReturn("hash");
        CommandLineRunner bootstrap = config.criarAdministradorInicial(pessoas, encoder,
                "admin@example.invalid", "senha-forte-123", "Administrador");
        bootstrap.run();
        ArgumentCaptor<Pessoa> captor = ArgumentCaptor.forClass(Pessoa.class);
        verify(pessoas).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(captor.getValue().getSenha()).isEqualTo("hash");
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@example.invalid");
    }

    @Test void naoRecriaAdministradorExistente() throws Exception {
        when(pessoas.existsByRole(Role.ADMIN)).thenReturn(true);
        config.criarAdministradorInicial(pessoas, encoder, "", "", "Administrador").run();
        verify(pessoas, never()).save(any());
    }

    @Test void falhaSemSenhaForteOuComEmailDeOutraConta() {
        assertThatThrownBy(() -> config.criarAdministradorInicial(pessoas, encoder,
                "admin@example.invalid", "curta", "Administrador").run())
                .isInstanceOf(IllegalStateException.class);
        when(pessoas.existsByEmail("admin@example.invalid")).thenReturn(true);
        assertThatThrownBy(() -> config.criarAdministradorInicial(pessoas, encoder,
                "admin@example.invalid", "senha-forte-123", "Administrador").run())
                .isInstanceOf(IllegalStateException.class);
        verify(pessoas, never()).save(any());
    }
}
