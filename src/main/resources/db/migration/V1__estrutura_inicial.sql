-- Schema inicial da alpha. Evoluções devem usar novas migrações, sem editar esta após aplicação.
CREATE TABLE pessoa (
    id_pessoa BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255), email VARCHAR(255), telefone VARCHAR(255), cpf VARCHAR(255),
    senha VARCHAR(255), role ENUM('ADMIN', 'USER'),
    cadastro_token_hash VARCHAR(64), cadastro_token_expira_em DATETIME(6),
    CONSTRAINT uk_pessoa_email UNIQUE (email),
    CONSTRAINT uk_pessoa_cadastro_token UNIQUE (cadastro_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE aluno (
    id_pessoa BIGINT NOT NULL PRIMARY KEY, instituicao VARCHAR(255), matricula VARCHAR(255),
    CONSTRAINT fk_aluno_pessoa FOREIGN KEY (id_pessoa) REFERENCES pessoa (id_pessoa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE professor (
    id_pessoa BIGINT NOT NULL PRIMARY KEY, area_formacao VARCHAR(255), titulacao VARCHAR(255),
    CONSTRAINT fk_professor_pessoa FOREIGN KEY (id_pessoa) REFERENCES pessoa (id_pessoa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE palestrante (
    id_pessoa BIGINT NOT NULL PRIMARY KEY, resumo_profissional VARCHAR(255),
    empresa_atual VARCHAR(255), cargo VARCHAR(255),
    CONSTRAINT fk_palestrante_pessoa FOREIGN KEY (id_pessoa) REFERENCES pessoa (id_pessoa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organizador (
    id_pessoa BIGINT NOT NULL PRIMARY KEY, instituicao VARCHAR(255), cargo VARCHAR(255),
    CONSTRAINT fk_organizador_pessoa FOREIGN KEY (id_pessoa) REFERENCES pessoa (id_pessoa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE colaborador (
    id_pessoa BIGINT NOT NULL PRIMARY KEY, funcao VARCHAR(255),
    disponibilidade VARCHAR(255), tipo VARCHAR(255),
    CONSTRAINT fk_colaborador_pessoa FOREIGN KEY (id_pessoa) REFERENCES pessoa (id_pessoa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE endereco (
    id_endereco BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    estado VARCHAR(255), cidade VARCHAR(255), bairro VARCHAR(255),
    logradouro VARCHAR(255), numero INT NOT NULL, complemento VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE local (
    id_local BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255),
    descricao VARCHAR(255), capacidade INT NOT NULL, id_endereco BIGINT,
    CONSTRAINT fk_local_endereco FOREIGN KEY (id_endereco) REFERENCES endereco (id_endereco)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE disciplina (
    id_disciplina BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255),
    descricao VARCHAR(255), turno ENUM('MATUTINO', 'NORTUNO', 'VESPERTINO'), id_professor BIGINT,
    CONSTRAINT fk_disciplina_professor FOREIGN KEY (id_professor) REFERENCES professor (id_pessoa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE patrocinador (
    id_patrocinador BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255),
    cnpj VARCHAR(255), valor_patrocinio DOUBLE NOT NULL, email VARCHAR(255),
    telefone VARCHAR(255), site VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evento (
    id_evento BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, tema VARCHAR(255), data DATE,
    horario_inicio VARCHAR(255), horario_fim VARCHAR(255), descricao VARCHAR(255),
    modalidade ENUM('ONLINE', 'PRESENCIAL'),
    status ENUM('CANCELADO', 'CRIADO', 'EM_ANDAMENTO', 'FINALIZADO'),
    id_disciplina BIGINT, id_patrocinador BIGINT, id_local BIGINT,
    qr_code_inscricao_url VARCHAR(255), qr_code_confirmacao_url VARCHAR(255),
    CONSTRAINT fk_evento_disciplina FOREIGN KEY (id_disciplina) REFERENCES disciplina (id_disciplina),
    CONSTRAINT fk_evento_patrocinador FOREIGN KEY (id_patrocinador) REFERENCES patrocinador (id_patrocinador),
    CONSTRAINT fk_evento_local FOREIGN KEY (id_local) REFERENCES local (id_local)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE participacao (
    id_participacao BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, inscricao INT NOT NULL,
    tipo VARCHAR(255), presente BIT(1) NOT NULL, id_evento BIGINT NOT NULL, id_pessoa BIGINT NOT NULL,
    CONSTRAINT uk_participacao_evento_pessoa UNIQUE (id_evento, id_pessoa),
    CONSTRAINT fk_participacao_evento FOREIGN KEY (id_evento) REFERENCES evento (id_evento),
    CONSTRAINT fk_participacao_pessoa FOREIGN KEY (id_pessoa) REFERENCES pessoa (id_pessoa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE certificado (
    id_certificado BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, data_emissao DATE,
    assinatura VARCHAR(255), codigo_validacao VARCHAR(36), url_publica VARCHAR(255),
    caminho_pdf VARCHAR(255), caminho_assinatura_visual VARCHAR(255), id_participacao BIGINT,
    CONSTRAINT uk_certificado_participacao UNIQUE (id_participacao),
    CONSTRAINT uk_certificado_codigo_validacao UNIQUE (codigo_validacao),
    CONSTRAINT uk_certificado_url_publica UNIQUE (url_publica),
    CONSTRAINT fk_certificado_participacao FOREIGN KEY (id_participacao) REFERENCES participacao (id_participacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medalha (
    id_medalha BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(255), descricao VARCHAR(255),
    tipo VARCHAR(20) NOT NULL DEFAULT 'BRONZE', id_participacao BIGINT,
    participacao_presenca_id BIGINT,
    CONSTRAINT uk_medalha_presenca UNIQUE (participacao_presenca_id),
    CONSTRAINT fk_medalha_participacao FOREIGN KEY (id_participacao) REFERENCES participacao (id_participacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sequencia_inscricao (
    id BIGINT NOT NULL PRIMARY KEY, valor BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sequencia_inscricao (id, valor) VALUES (1, 0);
