package com.fatec.muttley.massa;

import java.util.List;
import java.util.Locale;

/** Conteúdo sintético para navegação, filtros, paginação e diferentes perfis. */
final class CatalogoMassa {
    static final String SENHA = "Muttley-Teste-2026!";
    static final String CONVITE_VALIDO = "muttley-massa-convite-valido-2026";
    static final String CONVITE_EXPIRADO = "muttley-massa-convite-expirado-2026";
    static final String CONVITE_CONSUMIDO = "muttley-massa-convite-consumido-2026";
    static final String[] NOMES = {"Ana", "Bruno", "Carla", "Diego", "Elisa", "Fábio", "Gabriela", "Henrique"};
    static final String[] SOBRENOMES = {"Souza", "Lima", "Oliveira", "Santos", "Almeida", "Costa", "Pereira", "Rocha"};
    static final List<String> DISCIPLINAS = List.of("Programação Web", "Banco de Dados", "Engenharia de Software",
            "Interação Humano-Computador", "Redes de Computadores", "Segurança da Informação", "Inteligência Artificial",
            "Gestão de Projetos", "Computação em Nuvem", "Acessibilidade Digital", "Ciência de Dados", "Empreendedorismo");
    static final List<String> TEMAS = List.of("Primeiros passos no desenvolvimento web", "Oficina de prototipação de interfaces",
            "SQL na prática: do modelo à consulta", "Acessibilidade para produtos digitais", "Arquitetura de APIs REST",
            "Segurança de aplicações e proteção de dados", "Carreira e portfólio em tecnologia", "Inteligência artificial responsável",
            "Git em equipe: revisão e integração", "Containers e ambientes de desenvolvimento", "Testes automatizados com Java",
            "Visualização de dados para tomada de decisão");

    private CatalogoMassa() {}

    static String cpf(int indice) {
        String base = String.format(Locale.ROOT, "%09d", 800000000 + indice);
        String primeiro = base + digito(base, new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2});
        return primeiro + digito(primeiro, new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
    }

    static String cnpj(int indice) {
        String base = String.format(Locale.ROOT, "%08d0001", 80000000 + indice);
        String primeiro = base + digito(base, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return primeiro + digito(primeiro, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
    }

    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int indice = 0; indice < pesos.length; indice++) {
            soma += (base.charAt(indice) - '0') * pesos[indice];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
