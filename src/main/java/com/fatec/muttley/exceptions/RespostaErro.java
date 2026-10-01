package com.fatec.muttley.exceptions;

import java.util.List;

/** Contrato único para erros HTTP da API, inclusive os produzidos pelo filtro de segurança. */
public record RespostaErro(int status, String codigo, String erro, List<String> erros, String caminho) {
    public RespostaErro {
        erros = List.copyOf(erros);
    }
}
