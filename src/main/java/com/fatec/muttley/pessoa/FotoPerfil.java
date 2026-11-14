package com.fatec.muttley.pessoa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foto_perfil")
@Getter
@Setter
@NoArgsConstructor
public class FotoPerfil {
    @Id
    @Column(name = "id_pessoa")
    private Long pessoaId;

    @Column(name = "imagem_base64", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String imagemBase64;

    @Column(nullable = false)
    private String mime;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;
}
