package com.fatec.muttley.conquista;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras das medalhas, calculadas a partir das presenças em eventos concluídos (nada é gravado por evento):
 * trilhas que sobem de nível em marcos, especiais por semestre fechado e a disputa do semestre em andamento.
 */
public final class Conquistas {
    public static final int[] MARCOS_GERAL = {1, 10, 25};
    public static final int[] MARCOS_DISCIPLINA = {1, 5, 10};
    public static final int[] MARCOS_HORAS = {10, 30, 60};
    /** Mínimo de presenças no semestre para disputar Diamante ou Especialista. */
    public static final int MINIMO_ESPECIAL = 3;

    private static final String[] NIVEIS = {"BRONZE", "PRATA", "OURO"};
    private static final Set<String> TIPOS_PALESTRANTE = Set.of("palestrante", "organizador");

    private Conquistas() {
    }

    /** Uma presença confirmada em evento concluído. */
    public record Presenca(Long pessoaId, String pessoaNome, LocalDate data, int minutos, String disciplina, String tipo) {
    }

    public record Trilha(String chave, String titulo, String metrica, int valor, String nivel,
                         String proximoNivel, Integer proximoMarco, int[] marcos) {
    }

    public record Especial(String tipo, String titulo, String descricao, String semestre) {
    }

    public record Disputa(String titulo, String semestre, int posicao, int valor, int minimo) {
    }

    public record Destaque(String nome, int presencas) {
    }

    /** Semestre letivo pela data do evento: janeiro a junho é o 1º, julho a dezembro o 2º. */
    public record Semestre(int ano, int periodo) implements Comparable<Semestre> {
        public static Semestre de(LocalDate data) {
            return new Semestre(data.getYear(), data.getMonthValue() <= 6 ? 1 : 2);
        }

        public LocalDate fim() {
            return periodo == 1 ? LocalDate.of(ano, 6, 30) : LocalDate.of(ano, 12, 31);
        }

        public boolean encerradoEm(LocalDate hoje) {
            return hoje.isAfter(fim());
        }

        public String rotulo() {
            return ano + "/" + periodo;
        }

        @Override
        public int compareTo(Semestre outro) {
            return Comparator.comparingInt(Semestre::ano).thenComparingInt(Semestre::periodo).compare(this, outro);
        }
    }

    public static List<Trilha> trilhas(List<Presenca> minhas) {
        List<Trilha> trilhas = new ArrayList<>();
        trilhas.add(trilha("geral", "Participação", "PRESENCAS", minhas.size(), MARCOS_GERAL));
        int horas = minhas.stream().mapToInt(Presenca::minutos).sum() / 60;
        trilhas.add(trilha("horas", "Horas em eventos", "HORAS", horas, MARCOS_HORAS));
        contarPor(minhas, Presenca::disciplina).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> trilhas.add(trilha("disciplina:" + e.getKey(), e.getKey(), "PRESENCAS", e.getValue(), MARCOS_DISCIPLINA)));
        return trilhas;
    }

    static Trilha trilha(String chave, String titulo, String metrica, int valor, int[] marcos) {
        int alcancados = 0;
        while (alcancados < marcos.length && valor >= marcos[alcancados]) alcancados++;
        return new Trilha(chave, titulo, metrica, valor,
                alcancados == 0 ? null : NIVEIS[alcancados - 1],
                alcancados == marcos.length ? null : NIVEIS[alcancados],
                alcancados == marcos.length ? null : marcos[alcancados],
                marcos);
    }

    /** Especiais definitivas: só de semestres já encerrados (o passado não muda) e Palestrante. */
    public static List<Especial> especiais(Long pessoaId, List<Presenca> todas, LocalDate hoje) {
        List<Especial> especiais = new ArrayList<>();
        for (var e : porSemestre(todas).entrySet()) {
            Semestre semestre = e.getKey();
            if (!semestre.encerradoEm(hoje)) continue;
            List<Presenca> doSemestre = e.getValue();
            if (vencedores(doSemestre).contains(pessoaId)) {
                especiais.add(new Especial("DIAMANTE", "Diamante " + semestre.rotulo(),
                        "Mais presenças em eventos da Fatec no semestre.", semestre.rotulo()));
            }
            for (var porDisciplina : agrupar(doSemestre, Presenca::disciplina).entrySet()) {
                if (vencedores(porDisciplina.getValue()).contains(pessoaId)) {
                    especiais.add(new Especial("ESPECIALISTA",
                            "Especialista em " + porDisciplina.getKey() + " " + semestre.rotulo(),
                            "Mais presenças em eventos de " + porDisciplina.getKey() + " no semestre.", semestre.rotulo()));
                }
            }
        }
        boolean palestrou = todas.stream().anyMatch(p -> Objects.equals(p.pessoaId(), pessoaId)
                && p.tipo() != null && TIPOS_PALESTRANTE.contains(p.tipo().toLowerCase()));
        if (palestrou) {
            especiais.add(new Especial("PALESTRANTE", "Palestrante",
                    "Palestrou ou organizou um evento na Fatec.", null));
        }
        return especiais;
    }

    /** Posição da pessoa nas disputas do semestre em andamento (geral e por disciplina em que já participou). */
    public static List<Disputa> disputas(Long pessoaId, List<Presenca> todas, LocalDate hoje) {
        Semestre atual = Semestre.de(hoje);
        List<Presenca> doSemestre = todas.stream().filter(p -> Semestre.de(p.data()).equals(atual)).toList();
        List<Disputa> disputas = new ArrayList<>();
        Map<Long, Integer> geral = contarPor(doSemestre, Presenca::pessoaId);
        if (geral.containsKey(pessoaId)) {
            disputas.add(new Disputa("Diamante " + atual.rotulo(), atual.rotulo(),
                    posicao(geral, pessoaId), geral.get(pessoaId), MINIMO_ESPECIAL));
        }
        for (var e : agrupar(doSemestre, Presenca::disciplina).entrySet()) {
            Map<Long, Integer> contagem = contarPor(e.getValue(), Presenca::pessoaId);
            if (contagem.containsKey(pessoaId)) {
                disputas.add(new Disputa("Especialista em " + e.getKey() + " " + atual.rotulo(), atual.rotulo(),
                        posicao(contagem, pessoaId), contagem.get(pessoaId), MINIMO_ESPECIAL));
            }
        }
        return disputas;
    }

    /** Quem mais participou no semestre em andamento, para a coordenação. */
    public static List<Destaque> destaques(List<Presenca> todas, LocalDate hoje, int limite) {
        Semestre atual = Semestre.de(hoje);
        Map<Long, String> nomes = new LinkedHashMap<>();
        todas.forEach(p -> nomes.putIfAbsent(p.pessoaId(), p.pessoaNome()));
        return contarPor(todas.stream().filter(p -> Semestre.de(p.data()).equals(atual)).toList(), Presenca::pessoaId)
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()
                        .thenComparing(e -> nomes.getOrDefault(e.getKey(), "")))
                .limit(limite)
                .map(e -> new Destaque(nomes.get(e.getKey()), e.getValue()))
                .toList();
    }

    /** Todos os empatados no maior número de presenças, desde que alcancem o mínimo. */
    static Set<Long> vencedores(List<Presenca> presencas) {
        Map<Long, Integer> contagem = contarPor(presencas, Presenca::pessoaId);
        int maior = contagem.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (maior < MINIMO_ESPECIAL) return Set.of();
        return contagem.entrySet().stream().filter(e -> e.getValue() == maior)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    /** Classificação com empates: 1 + quantas pessoas têm mais presenças. */
    static int posicao(Map<Long, Integer> contagem, Long pessoaId) {
        int minha = contagem.get(pessoaId);
        return 1 + (int) contagem.values().stream().filter(v -> v > minha).count();
    }

    private static Map<Semestre, List<Presenca>> porSemestre(List<Presenca> todas) {
        Map<Semestre, List<Presenca>> mapa = new LinkedHashMap<>();
        new TreeSet<>(todas.stream().map(p -> Semestre.de(p.data())).toList())
                .forEach(s -> mapa.put(s, todas.stream().filter(p -> Semestre.de(p.data()).equals(s)).toList()));
        return mapa;
    }

    /** Agrupa ignorando presenças sem a chave (ex.: evento sem disciplina). */
    private static <K> Map<K, List<Presenca>> agrupar(List<Presenca> presencas, Function<Presenca, K> chave) {
        return presencas.stream().filter(p -> chave.apply(p) != null)
                .collect(Collectors.groupingBy(chave, LinkedHashMap::new, Collectors.toList()));
    }

    private static <K> Map<K, Integer> contarPor(List<Presenca> presencas, Function<Presenca, K> chave) {
        Map<K, Integer> contagem = new LinkedHashMap<>();
        agrupar(presencas, chave).forEach((k, lista) -> contagem.put(k, lista.size()));
        return contagem;
    }
}
