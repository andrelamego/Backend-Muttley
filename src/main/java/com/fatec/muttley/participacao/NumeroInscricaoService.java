package com.fatec.muttley.participacao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NumeroInscricaoService {
    private final JdbcTemplate jdbc;
    public NumeroInscricaoService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // Reserva independente: números não são reutilizados se a inscrição sofrer rollback.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int proximo() {
        jdbc.update("insert ignore into sequencia_inscricao (id, valor) values (1, 0)");
        long atual = jdbc.queryForObject("select valor from sequencia_inscricao where id = 1 for update", Long.class);
        long maior = jdbc.queryForObject("select coalesce(max(inscricao), 0) from participacao", Long.class);
        int proximo = Math.toIntExact(Math.max(atual, maior) + 1);
        jdbc.update("update sequencia_inscricao set valor = ? where id = 1", proximo);
        return proximo;
    }
}
