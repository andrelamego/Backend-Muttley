package com.fatec.muttley.certificado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aparência do certificado escolhida pela coordenação; existe uma única linha (id 1). */
@Entity
@Table(name = "modelo_certificado")
@Getter
@Setter
@NoArgsConstructor
public class ModeloCertificado {
    public static final long ID_UNICO = 1L;

    @Id
    @Column(name = "id_modelo_certificado")
    private Long id = ID_UNICO;
    private String instituicao;
    private String titulo;
    private String texto;
    @Column(name = "cargo_assinatura")
    private String cargoAssinatura;
    @Column(name = "cor_principal")
    private String corPrincipal;
    @Column(name = "cor_destaque")
    private String corDestaque;
    @Enumerated(EnumType.STRING)
    private LeiauteCertificado leiaute;
    @Column(name = "logo_base64", columnDefinition = "MEDIUMTEXT")
    private String logoBase64;
    @Column(name = "logo_mime")
    private String logoMime;

    /** Mesmos valores da migração V2; usado se a linha não existir. */
    public static ModeloCertificado padrao() {
        ModeloCertificado m = new ModeloCertificado();
        m.setInstituicao("Fatec Zona Leste");
        m.setTitulo("Certificado de participação");
        m.setTexto("Participou como {tipo} do evento {evento}, realizado em {data}, com carga horária de {carga}.");
        m.setCargoAssinatura("Coordenação do curso de Análise e Desenvolvimento de Sistemas da Fatec Zona Leste");
        m.setCorPrincipal("#2236e0");
        m.setCorDestaque("#ffd93b");
        m.setLeiaute(LeiauteCertificado.INGRESSO);
        return m;
    }
}
