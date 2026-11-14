package com.fatec.muttley.conquista;

import com.fatec.muttley.conquista.Conquistas.Presenca;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConquistasTest {
    private static final LocalDate SEM1 = LocalDate.of(2026, 3, 10);
    private static final LocalDate SEM2 = LocalDate.of(2026, 9, 10);

    private static List<Presenca> vezes(long pessoa, int n, LocalDate data, String disciplina) {
        List<Presenca> lista = new ArrayList<>();
        for (int i = 0; i < n; i++) lista.add(new Presenca(pessoa, "Pessoa " + pessoa, data, 120, disciplina, "Participante"));
        return lista;
    }

    @Test void trilhaSobeNosMarcosEApontaOProximo() {
        var nenhuma = Conquistas.trilha("geral", "Participação", "PRESENCAS", 0, Conquistas.MARCOS_GERAL);
        assertThat(nenhuma.nivel()).isNull();
        assertThat(nenhuma.proximoNivel()).isEqualTo("BRONZE");
        assertThat(nenhuma.proximoMarco()).isEqualTo(1);

        var prata = Conquistas.trilha("geral", "Participação", "PRESENCAS", 12, Conquistas.MARCOS_GERAL);
        assertThat(prata.nivel()).isEqualTo("PRATA");
        assertThat(prata.proximoMarco()).isEqualTo(25);

        var ouro = Conquistas.trilha("geral", "Participação", "PRESENCAS", 40, Conquistas.MARCOS_GERAL);
        assertThat(ouro.nivel()).isEqualTo("OURO");
        assertThat(ouro.proximoNivel()).isNull();
        assertThat(ouro.proximoMarco()).isNull();
    }

    @Test void trilhasGeralHorasEUmaPorDisciplinaIgnorandoEventoSemDisciplina() {
        List<Presenca> minhas = new ArrayList<>(vezes(1, 5, SEM1, "Banco de Dados"));
        minhas.addAll(vezes(1, 1, SEM1, "Engenharia de Software"));
        minhas.addAll(vezes(1, 1, SEM1, null));
        var trilhas = Conquistas.trilhas(minhas);

        assertThat(trilhas).extracting(Conquistas.Trilha::chave)
                .containsExactly("geral", "horas", "disciplina:Banco de Dados", "disciplina:Engenharia de Software");
        assertThat(trilhas.get(0).valor()).isEqualTo(7);
        assertThat(trilhas.get(1).valor()).isEqualTo(14); // 7 eventos de 2 h
        assertThat(trilhas.get(1).nivel()).isEqualTo("BRONZE");
        assertThat(trilhas.get(2).nivel()).isEqualTo("PRATA"); // 5 presenças em Banco de Dados
    }

    @Test void especiaisSoDeSemestreEncerradoComEmpateEMinimo() {
        List<Presenca> todas = new ArrayList<>();
        todas.addAll(vezes(1, 4, SEM1, "Banco de Dados"));
        todas.addAll(vezes(2, 4, SEM1, "Banco de Dados")); // empate: os dois levam
        todas.addAll(vezes(3, 2, SEM1, "Redes")); // vence Redes, mas abaixo do mínimo
        todas.addAll(vezes(1, 5, SEM2, "Banco de Dados")); // semestre ainda em andamento

        var hoje = LocalDate.of(2026, 11, 14);
        assertThat(Conquistas.especiais(1L, todas, hoje)).extracting(Conquistas.Especial::titulo)
                .containsExactly("Diamante 2026/1", "Especialista em Banco de Dados 2026/1");
        assertThat(Conquistas.especiais(2L, todas, hoje)).extracting(Conquistas.Especial::titulo)
                .contains("Diamante 2026/1");
        assertThat(Conquistas.especiais(3L, todas, hoje)).isEmpty();

        // Depois que o 2º semestre fecha, a pessoa 1 também leva o de 2026/2
        assertThat(Conquistas.especiais(1L, todas, LocalDate.of(2027, 1, 2))).extracting(Conquistas.Especial::titulo)
                .contains("Diamante 2026/2");
    }

    @Test void palestranteVemDoTipoDaParticipacao() {
        var todas = List.of(new Presenca(9L, "Nove", SEM2, 60, null, "Palestrante"));
        assertThat(Conquistas.especiais(9L, todas, SEM2)).extracting(Conquistas.Especial::tipo).containsExactly("PALESTRANTE");
    }

    @Test void disputaDoSemestreAtualComPosicaoEmpatada() {
        List<Presenca> todas = new ArrayList<>();
        todas.addAll(vezes(1, 3, SEM2, "Redes"));
        todas.addAll(vezes(2, 5, SEM2, "Redes"));
        todas.addAll(vezes(3, 3, SEM2, "Redes"));
        todas.addAll(vezes(1, 9, SEM1, "Redes")); // semestre passado não conta na disputa

        var disputas = Conquistas.disputas(3L, todas, SEM2);
        assertThat(disputas).extracting(Conquistas.Disputa::titulo)
                .containsExactly("Diamante 2026/2", "Especialista em Redes 2026/2");
        assertThat(disputas.get(0).posicao()).isEqualTo(2); // empatado com a pessoa 1, atrás da 2
        assertThat(disputas.get(0).valor()).isEqualTo(3);
        assertThat(Conquistas.disputas(4L, todas, SEM2)).isEmpty();
    }

    @Test void destaquesDoSemestreOrdenadosELimitados() {
        List<Presenca> todas = new ArrayList<>();
        todas.addAll(vezes(1, 2, SEM2, null));
        todas.addAll(vezes(2, 4, SEM2, null));
        todas.addAll(vezes(3, 1, SEM2, null));
        assertThat(Conquistas.destaques(todas, SEM2, 2)).containsExactly(
                new Conquistas.Destaque("Pessoa 2", 4), new Conquistas.Destaque("Pessoa 1", 2));
    }
}
