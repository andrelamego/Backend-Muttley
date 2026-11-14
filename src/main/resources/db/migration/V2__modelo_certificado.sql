-- Modelo visual do certificado, editável pela coordenação. Há sempre uma única linha (id 1).
CREATE TABLE modelo_certificado (
    id_modelo_certificado BIGINT NOT NULL PRIMARY KEY,
    instituicao VARCHAR(255) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    texto VARCHAR(1000) NOT NULL,
    cargo_assinatura VARCHAR(255) NOT NULL,
    cor_principal VARCHAR(7) NOT NULL,
    cor_destaque VARCHAR(7) NOT NULL,
    leiaute ENUM('CLASSICO', 'INGRESSO') NOT NULL,
    logo_base64 MEDIUMTEXT,
    logo_mime VARCHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO modelo_certificado
    (id_modelo_certificado, instituicao, titulo, texto, cargo_assinatura, cor_principal, cor_destaque, leiaute)
VALUES
    (1, 'Fatec Zona Leste', 'Certificado de participação',
     'Participou como {tipo} do evento {evento}, realizado em {data}, com carga horária de {carga}.',
     'Coordenação do curso de Análise e Desenvolvimento de Sistemas da Fatec Zona Leste',
     '#2236e0', '#ffd93b', 'INGRESSO');
