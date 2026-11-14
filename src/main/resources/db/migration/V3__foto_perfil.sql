-- Foto de perfil em tabela própria, para não pesar cada leitura de pessoa. Some junto com a pessoa.
CREATE TABLE foto_perfil (
    id_pessoa BIGINT NOT NULL PRIMARY KEY,
    imagem_base64 MEDIUMTEXT NOT NULL,
    mime VARCHAR(20) NOT NULL,
    atualizada_em DATETIME(6) NOT NULL,
    CONSTRAINT fk_foto_perfil_pessoa FOREIGN KEY (id_pessoa) REFERENCES pessoa (id_pessoa) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
