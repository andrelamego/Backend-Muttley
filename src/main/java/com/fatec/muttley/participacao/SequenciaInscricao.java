package com.fatec.muttley.participacao;

import jakarta.persistence.*;

@Entity
@Table(name = "sequencia_inscricao")
public class SequenciaInscricao {
    @Id private Long id;
    @Column(nullable = false) private long valor;
}
