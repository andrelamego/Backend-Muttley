package com.fatec.muttley.painel;

import com.fatec.muttley.evento.enums.StatusEventoEnum;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.fatec.muttley.evento.enums.StatusEventoEnum.CRIADO;
import static com.fatec.muttley.evento.enums.StatusEventoEnum.EM_ANDAMENTO;
import static com.fatec.muttley.evento.enums.StatusEventoEnum.FINALIZADO;
import static org.assertj.core.api.Assertions.assertThat;

class PainelAdminTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 15);

    private static EstatisticaEvento ev(long id, LocalDate data, StatusEventoEnum status, String disciplina,
                                        Integer capacidade, long inscritos, long presentes) {
        return new EstatisticaEvento(id, "Evento " + id, data, "14:00", "16:00", status, disciplina,
                capacidade, inscritos, presentes);
    }

    @Test void indicadoresDoSemestreContamSoEventosRealizadosNaTaxa() {
        var eventos = List.of(
                ev(1, LocalDate.of(2026, 9, 10), FINALIZADO, "BD", 40, 20, 15),
                ev(2, LocalDate.of(2026, 10, 1), FINALIZADO, "BD", 40, 10, 5),
                ev(3, LocalDate.of(2026, 11, 20), CRIADO, "Redes", 30, 8, 0),
                ev(4, LocalDate.of(2026, 3, 5), FINALIZADO, "BD", 40, 10, 10));
        var emissoes = List.of(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 3, 6));

        var r = PainelAdmin.montar(eventos, emissoes, HOJE, 4);

        assertThat(r.semestre().semestre()).isEqualTo("2026/2");
        assertThat(r.semestre().eventosRealizados()).isEqualTo(2);
        assertThat(r.semestre().inscricoes()).isEqualTo(38); // inclui o evento ainda aberto
        assertThat(r.semestre().presentes()).isEqualTo(20);
        assertThat(r.semestre().taxaPresenca()).isEqualTo(67); // 20 de 30 inscritos nos realizados
        assertThat(r.semestre().certificados()).isEqualTo(2);
        assertThat(r.semestreAnterior().semestre()).isEqualTo("2026/1");
        assertThat(r.semestreAnterior().taxaPresenca()).isEqualTo(100);
        assertThat(r.pessoasSemConta()).isEqualTo(4);
    }

    @Test void ocupacaoSoDeEventosAbertosDeHojeEmDiante() {
        var eventos = List.of(
                ev(1, HOJE.plusDays(2), CRIADO, "BD", 40, 10, 0),
                ev(2, HOJE, EM_ANDAMENTO, "BD", null, 5, 2),
                ev(3, HOJE.minusDays(1), CRIADO, "BD", 40, 10, 0),
                ev(4, HOJE.plusDays(1), FINALIZADO, "BD", 40, 10, 0));

        var ocupacao = PainelAdmin.ocupacao(eventos, HOJE);

        assertThat(ocupacao).extracting(PainelAdmin.Ocupacao::id).containsExactly(2L, 1L);
        assertThat(ocupacao.get(0).percentual()).isNull(); // local sem capacidade
        assertThat(ocupacao.get(1).percentual()).isEqualTo(25);
    }

    @Test void serieMensalTemSeisMesesEmOrdemComPresencasPeloMesDoEvento() {
        var eventos = List.of(
                ev(1, LocalDate.of(2026, 11, 2), FINALIZADO, "BD", 40, 10, 7),
                ev(2, LocalDate.of(2026, 6, 2), FINALIZADO, "BD", 40, 10, 9), // fora da janela
                ev(3, LocalDate.of(2026, 11, 20), CRIADO, "BD", 40, 10, 0));
        var serie = PainelAdmin.serieMensal(eventos, List.of(LocalDate.of(2026, 7, 1)), HOJE);

        assertThat(serie).extracting(PainelAdmin.Mes::mes)
                .containsExactly("2026-06", "2026-07", "2026-08", "2026-09", "2026-10", "2026-11");
        assertThat(serie.get(5).presencas()).isEqualTo(7);
        assertThat(serie.get(0).presencas()).isEqualTo(9);
        assertThat(serie.get(1).certificados()).isEqualTo(1);
        assertThat(serie.get(5).rotulo()).isEqualTo("nov");
    }

    @Test void presencaRecenteEPorDisciplinaOrdenados() {
        var eventos = List.of(
                ev(1, LocalDate.of(2026, 9, 1), FINALIZADO, "BD", 40, 10, 4),
                ev(2, LocalDate.of(2026, 10, 1), FINALIZADO, "Redes", 40, 0, 0),
                ev(3, LocalDate.of(2026, 10, 5), FINALIZADO, null, 40, 10, 9));

        assertThat(PainelAdmin.presencaRecentes(eventos)).extracting(PainelAdmin.PresencaEvento::id)
                .containsExactly(3L, 2L, 1L);
        assertThat(PainelAdmin.presencaRecentes(eventos).get(1).taxa()).isZero(); // sem inscritos, sem divisão por zero

        var disciplinas = PainelAdmin.montar(eventos, List.of(), HOJE, 0).porDisciplina();
        assertThat(disciplinas).extracting(PainelAdmin.PorDisciplina::disciplina)
                .containsExactly("Sem disciplina", "BD", "Redes");
    }

    @Test void eventosDeHojeOrdenadosPeloHorario() {
        var tarde = new EstatisticaEvento(1L, "Tarde", HOJE, "19:00", "21:00", CRIADO, null, 30, 3L, 0L);
        var manha = new EstatisticaEvento(2L, "Manhã", HOJE, "09:00", "11:00", EM_ANDAMENTO, null, 30, 5L, 2L);
        assertThat(PainelAdmin.hoje(List.of(tarde, manha), HOJE)).extracting(PainelAdmin.EventoHoje::id)
                .containsExactly(2L, 1L);
    }

    @Test void buscaDeEmissoesCobreOSemestreAnterior() {
        assertThat(PainelAdmin.inicioDaBusca(HOJE)).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(PainelAdmin.inicioDaBusca(LocalDate.of(2026, 3, 1))).isEqualTo(LocalDate.of(2025, 7, 1));
    }
}
