package com.fatec.muttley.auth;

import com.fatec.muttley.pessoa.AtualizacaoPessoa;
import com.fatec.muttley.pessoa.Pessoa;
import com.fatec.muttley.pessoa.PessoaRepository;
import com.fatec.muttley.pessoa.PessoaService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static com.fatec.muttley.support.Cenarios.dadosPessoa;
import static com.fatec.muttley.support.Cenarios.pessoa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastroConviteServiceTest {
    @Mock PessoaRepository pessoas;
    @Mock PessoaService pessoaService;
    CadastroConviteService service;
    Instant agora = Instant.parse("2026-09-25T12:00:00Z");

    @BeforeEach void preparar() {
        service = new CadastroConviteService(pessoas, pessoaService, Clock.fixed(agora, ZoneOffset.UTC));
    }

    @Test void conviteEOpacoValidoPor24HorasENaoSalvaTokenEmTextoClaro() {
        Pessoa pessoa = pessoa(1);
        pessoa.setSenha(null);
        String token = service.emitir(pessoa).orElseThrow();
        assertThat(token).hasSizeGreaterThanOrEqualTo(40);
        assertThat(pessoa.getCadastroTokenHash()).hasSize(64).isNotEqualTo(token);
        assertThat(pessoa.getCadastroTokenExpiraEm()).isEqualTo(agora.plus(Duration.ofHours(24)));
        when(pessoas.findByCadastroTokenHash(pessoa.getCadastroTokenHash())).thenReturn(Optional.of(pessoa));
        assertThat(service.consultar(token).email()).isEqualTo(pessoa.getEmail());
    }

    @Test void conviteExpiradoNaoRevelaDadosNemCompletaCadastro() {
        Pessoa pessoa = pessoa(1);
        pessoa.setSenha(null);
        String token = service.emitir(pessoa).orElseThrow();
        pessoa.setCadastroTokenExpiraEm(agora);
        when(pessoas.findByCadastroTokenHash(pessoa.getCadastroTokenHash())).thenReturn(Optional.of(pessoa));
        when(pessoas.findWithLockByCadastroTokenHash(pessoa.getCadastroTokenHash())).thenReturn(Optional.of(pessoa));
        assertThatThrownBy(() -> service.consultar(token)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.concluir(token, dadosPessoa(null,"senha"))).isInstanceOf(ResponseStatusException.class);
        verify(pessoaService,never()).salvarOuAtualizar(any());
    }

    @Test void conclusaoConsomeConviteEImpedeTrocaDeIdentidade() {
        Pessoa pessoa = pessoa(1);
        AtualizacaoPessoa dados = dadosPessoa(null,"senha");
        pessoa.setEmail(dados.email());
        pessoa.setSenha(null);
        String token = service.emitir(pessoa).orElseThrow();
        String hash = pessoa.getCadastroTokenHash();
        when(pessoas.findWithLockByCadastroTokenHash(hash)).thenReturn(Optional.of(pessoa));
        when(pessoaService.salvarOuAtualizar(dados.withId(1L))).thenReturn(pessoa);
        when(pessoas.save(pessoa)).thenReturn(pessoa);
        assertThat(service.concluir(token,dados)).isSameAs(pessoa);
        assertThat(pessoa.getCadastroTokenHash()).isNull();
        assertThat(pessoa.getCadastroTokenExpiraEm()).isNull();
        verify(pessoaService).salvarOuAtualizar(dados.withId(1L));
    }

    @Test void cpfDiferenteNaoPodeTomarContaMesmoComConvite() {
        Pessoa pessoa = pessoa(1);
        AtualizacaoPessoa dados = dadosPessoa(null,"senha");
        pessoa.setEmail(dados.email());
        pessoa.setSenha(null);
        String token = service.emitir(pessoa).orElseThrow();
        when(pessoas.findWithLockByCadastroTokenHash(pessoa.getCadastroTokenHash())).thenReturn(Optional.of(pessoa));
        var falsificado = new AtualizacaoPessoa(null,dados.nome(),dados.email(),dados.telefone(),"111.444.777-35",dados.senha());
        assertThatThrownBy(() -> service.concluir(token,falsificado)).isInstanceOf(ResponseStatusException.class);
        verify(pessoaService,never()).salvarOuAtualizar(any());
    }

    @Test void cadastroDiretoDefineCpfSomenteDepoisDoConvite() {
        Pessoa pessoa = pessoa(1);
        AtualizacaoPessoa dados = dadosPessoa(null,"senha");
        pessoa.setEmail(dados.email());
        pessoa.setCpf(null);
        String token = service.emitir(pessoa).orElseThrow();
        when(pessoas.findWithLockByCadastroTokenHash(pessoa.getCadastroTokenHash())).thenReturn(Optional.of(pessoa));
        when(pessoaService.salvarOuAtualizar(dados.withId(1L))).thenReturn(pessoa);
        when(pessoas.save(pessoa)).thenReturn(pessoa);
        assertThat(service.concluir(token,dados)).isSameAs(pessoa);
        verify(pessoaService).salvarOuAtualizar(dados.withId(1L));
    }

    @Test void requisicaoRepetidaNaoInvalidaConviteAtivo() {
        Pessoa pessoa = pessoa(1);
        pessoa.setSenha(null);
        String token = service.emitir(pessoa).orElseThrow();
        String hash = pessoa.getCadastroTokenHash();
        assertThat(service.emitir(pessoa)).isEmpty();
        assertThat(pessoa.getCadastroTokenHash()).isEqualTo(hash);
        pessoa.setCadastroTokenExpiraEm(agora.minusSeconds(1));
        assertThat(service.emitir(pessoa)).isPresent().get().isNotEqualTo(token);
        assertThat(pessoa.getCadastroTokenHash()).isNotEqualTo(hash);
    }

    @Test void novaInscricaoGeraConviteMesmoComTokenAnteriorAtivo() {
        Pessoa pessoa = pessoa(1);
        pessoa.setSenha(null);
        String tokenAnterior = service.emitir(pessoa).orElseThrow();
        String hashAnterior = pessoa.getCadastroTokenHash();

        String novoToken = service.renovarParaInscricao(pessoa);

        assertThat(novoToken).isNotEqualTo(tokenAnterior);
        assertThat(pessoa.getCadastroTokenHash()).isNotEqualTo(hashAnterior);
        assertThat(pessoa.getCadastroTokenExpiraEm()).isEqualTo(agora.plus(Duration.ofHours(24)));
        verify(pessoas, org.mockito.Mockito.times(2)).save(pessoa);
    }

    @Test void contaConcluidaNaoRecebeNovoConviteNaInscricao() {
        Pessoa pessoa = pessoa(1);
        pessoa.setSenha("hash");

        assertThatThrownBy(() -> service.renovarParaInscricao(pessoa))
                .isInstanceOf(ResponseStatusException.class);
        verify(pessoas, never()).save(any());
    }
}
