package com.fatec.muttley.painel;

import com.fatec.muttley.conquista.Conquistas.Semestre;
import com.fatec.muttley.evento.enums.StatusEventoEnum;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cálculos do painel da coordenação a partir das contagens por evento e das
 * datas de emissão dos certificados. Puro: não fala com o banco.
 */
public final class PainelAdmin {
    static final int OCUPACAO_MAXIMO = 8;
    static final int RECENTES_MAXIMO = 6;
    static final int MESES = 6;
    static final int DISCIPLINAS_MAXIMO = 8;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final Set<StatusEventoEnum> ABERTOS = Set.of(StatusEventoEnum.CRIADO, StatusEventoEnum.EM_ANDAMENTO);

    private PainelAdmin() {
    }

    public record EventoHoje(Long id, String tema, String horarioInicio, String horarioFim,
                             StatusEventoEnum status, long inscritos, long presentes) {
    }

    public record Indicadores(String semestre, long eventosRealizados, long inscricoes, long presentes,
                              int taxaPresenca, long certificados) {
    }

    public record Ocupacao(Long id, String tema, LocalDate data, String horarioInicio,
                           long inscritos, Integer capacidade, Integer percentual) {
    }

    public record PresencaEvento(Long id, String tema, LocalDate data, long inscritos, long presentes, int taxa) {
    }

    public record Mes(String mes, String rotulo, long presencas, long certificados) {
    }

    public record PorDisciplina(String disciplina, long eventos, long presentes) {
    }

    public record Resultado(List<EventoHoje> hoje, Indicadores semestre, Indicadores semestreAnterior,
                            List<Ocupacao> ocupacao, List<PresencaEvento> presencaRecentes,
                            List<Mes> serieMensal, List<PorDisciplina> porDisciplina, long pessoasSemConta) {
    }

    /** Desde quando buscar emissões de certificado para cobrir os dois semestres e a série mensal. */
    public static LocalDate inicioDaBusca(LocalDate hoje) {
        LocalDate semestres = Semestre.de(hoje).anterior().inicio();
        LocalDate meses = YearMonth.from(hoje).minusMonths(MESES - 1L).atDay(1);
        return semestres.isBefore(meses) ? semestres : meses;
    }

    public static Resultado montar(List<EstatisticaEvento> eventos, List<LocalDate> emissoes,
                                   LocalDate hoje, long pessoasSemConta) {
        Semestre atual = Semestre.de(hoje);
        return new Resultado(
                hoje(eventos, hoje),
                indicadores(atual, eventos, emissoes),
                indicadores(atual.anterior(), eventos, emissoes),
                ocupacao(eventos, hoje),
                presencaRecentes(eventos),
                serieMensal(eventos, emissoes, hoje),
                porDisciplina(atual, eventos),
                pessoasSemConta);
    }

    static List<EventoHoje> hoje(List<EstatisticaEvento> eventos, LocalDate hoje) {
        return eventos.stream()
                .filter(e -> hoje.equals(e.data()))
                .sorted(Comparator.comparing(e -> textoOuVazio(e.horarioInicio())))
                .map(e -> new EventoHoje(e.id(), e.tema(), e.horarioInicio(), e.horarioFim(), e.status(),
                        e.inscritosOuZero(), e.presentesOuZero()))
                .toList();
    }

    static Indicadores indicadores(Semestre semestre, List<EstatisticaEvento> eventos, List<LocalDate> emissoes) {
        List<EstatisticaEvento> doSemestre = eventos.stream().filter(e -> semestre.contem(e.data())).toList();
        List<EstatisticaEvento> realizados = doSemestre.stream()
                .filter(e -> e.status() == StatusEventoEnum.FINALIZADO).toList();
        long inscritosRealizados = realizados.stream().mapToLong(EstatisticaEvento::inscritosOuZero).sum();
        long presentes = realizados.stream().mapToLong(EstatisticaEvento::presentesOuZero).sum();
        return new Indicadores(
                semestre.rotulo(),
                realizados.size(),
                doSemestre.stream().mapToLong(EstatisticaEvento::inscritosOuZero).sum(),
                presentes,
                taxa(presentes, inscritosRealizados),
                emissoes.stream().filter(semestre::contem).count());
    }

    static List<Ocupacao> ocupacao(List<EstatisticaEvento> eventos, LocalDate hoje) {
        return eventos.stream()
                .filter(e -> ABERTOS.contains(e.status()) && e.data() != null && !e.data().isBefore(hoje))
                .sorted(Comparator.comparing(EstatisticaEvento::data)
                        .thenComparing(e -> textoOuVazio(e.horarioInicio())))
                .limit(OCUPACAO_MAXIMO)
                .map(e -> new Ocupacao(e.id(), e.tema(), e.data(), e.horarioInicio(), e.inscritosOuZero(),
                        e.capacidade(),
                        e.capacidade() == null || e.capacidade() <= 0 ? null : taxa(e.inscritosOuZero(), e.capacidade())))
                .toList();
    }

    static List<PresencaEvento> presencaRecentes(List<EstatisticaEvento> eventos) {
        return eventos.stream()
                .filter(e -> e.status() == StatusEventoEnum.FINALIZADO && e.data() != null)
                .sorted(Comparator.comparing(EstatisticaEvento::data).reversed())
                .limit(RECENTES_MAXIMO)
                .map(e -> new PresencaEvento(e.id(), e.tema(), e.data(), e.inscritosOuZero(), e.presentesOuZero(),
                        taxa(e.presentesOuZero(), e.inscritosOuZero())))
                .toList();
    }

    static List<Mes> serieMensal(List<EstatisticaEvento> eventos, List<LocalDate> emissoes, LocalDate hoje) {
        Map<YearMonth, long[]> meses = new LinkedHashMap<>();
        YearMonth atual = YearMonth.from(hoje);
        for (int i = MESES - 1; i >= 0; i--) {
            meses.put(atual.minusMonths(i), new long[2]);
        }
        // Presenças pelo mês do evento: as inscrições não guardam a data em que foram feitas.
        for (EstatisticaEvento e : eventos) {
            if (e.status() != StatusEventoEnum.FINALIZADO || e.data() == null) continue;
            long[] mes = meses.get(YearMonth.from(e.data()));
            if (mes != null) mes[0] += e.presentesOuZero();
        }
        for (LocalDate emissao : emissoes) {
            if (emissao == null) continue;
            long[] mes = meses.get(YearMonth.from(emissao));
            if (mes != null) mes[1]++;
        }
        return meses.entrySet().stream()
                .map(m -> new Mes(m.getKey().toString(),
                        m.getKey().getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", ""),
                        m.getValue()[0], m.getValue()[1]))
                .toList();
    }

    static List<PorDisciplina> porDisciplina(Semestre semestre, List<EstatisticaEvento> eventos) {
        Map<String, long[]> porNome = new LinkedHashMap<>();
        for (EstatisticaEvento e : eventos) {
            if (e.status() != StatusEventoEnum.FINALIZADO || !semestre.contem(e.data())) continue;
            String nome = e.disciplina() == null ? "Sem disciplina" : e.disciplina();
            long[] totais = porNome.computeIfAbsent(nome, n -> new long[2]);
            totais[0]++;
            totais[1] += e.presentesOuZero();
        }
        return porNome.entrySet().stream()
                .map(d -> new PorDisciplina(d.getKey(), d.getValue()[0], d.getValue()[1]))
                .sorted(Comparator.comparingLong(PorDisciplina::presentes).reversed()
                        .thenComparing(PorDisciplina::disciplina))
                .limit(DISCIPLINAS_MAXIMO)
                .toList();
    }

    static int taxa(long parte, long total) {
        return total <= 0 ? 0 : Math.round(parte * 100f / total);
    }

    private static String textoOuVazio(String texto) {
        return texto == null ? "" : texto;
    }
}
