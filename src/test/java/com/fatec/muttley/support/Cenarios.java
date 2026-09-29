package com.fatec.muttley.support;

import com.fatec.muttley.evento.*;
import com.fatec.muttley.evento.enums.*;
import com.fatec.muttley.local.Local;
import com.fatec.muttley.participacao.*;
import com.fatec.muttley.pessoa.*;
import java.time.LocalDate;

/** Dados sintéticos; nenhum teste precisa de cadastro ou serviço externo. */
public final class Cenarios {
    private Cenarios() {}
    public static Pessoa pessoa(long id) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(id);
        pessoa.setNome("Participante de teste");
        pessoa.setCpf("529.982.247-25");
        pessoa.setEmail("pessoa" + id + "@example.invalid");
        pessoa.setRole(Role.USER);
        return pessoa;
    }
    public static Evento evento(StatusEventoEnum status) {
        Evento evento = new Evento();
        evento.setId(10L);
        Local local=new Local();local.setId(3L);local.setCapacidade(100);evento.setLocal(local);
        evento.setTema("Semana de tecnologia");
        evento.setDescricao("Evento de teste");
        evento.setData(LocalDate.now().plusDays(2));
        evento.setHorarioInicio("09:00");
        evento.setHorarioFim("11:30");
        evento.setStatus(status);
        return evento;
    }
    public static AtualizacaoEvento dadosEvento(Long id, String inicio, String fim) {
        return new AtualizacaoEvento(id, "Semana de tecnologia", "Evento de teste",
                LocalDate.now().plusDays(2), inicio, fim, ModalidadeEventoEnum.values()[0],
                StatusEventoEnum.FINALIZADO, 1L, 2L, 3L);
    }
    public static Participacao participacao(long id, boolean presente) {
        Participacao participacao = new Participacao();
        participacao.setId(id);
        participacao.setInscricao((int) id);
        participacao.setTipo("Participante");
        participacao.setPresente(presente);
        participacao.setPessoa(pessoa(id));
        participacao.setEvento(evento(StatusEventoEnum.EM_ANDAMENTO));
        return participacao;
    }
    public static AtualizacaoPessoa dadosPessoa(Long id, String senha) {
        return new AtualizacaoPessoa(id, "Pessoa de teste", "teste@example.invalid",
                "11999999999", "529.982.247-25", senha);
    }
}
