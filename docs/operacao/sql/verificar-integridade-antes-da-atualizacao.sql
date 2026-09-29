-- Somente leitura. Executar no banco desejado antes de preparar a migração.
-- Não corrige registros nem aplica alterações de esquema.

-- A unicidade de participacao impede repetir pessoa/evento.
SELECT id_evento, id_pessoa, COUNT(*) AS quantidade
FROM participacao
GROUP BY id_evento, id_pessoa
HAVING COUNT(*) > 1;

-- A unicidade de certificado impede mais de um por participacao.
SELECT id_participacao, COUNT(*) AS quantidade
FROM certificado
WHERE id_participacao IS NOT NULL
GROUP BY id_participacao
HAVING COUNT(*) > 1;

-- A unicidade de email ja faz parte do modelo de pessoa.
SELECT email, COUNT(*) AS quantidade
FROM pessoa
WHERE email IS NOT NULL
GROUP BY email
HAVING COUNT(*) > 1;

-- Referencia para inicializacao da sequencia de novas inscricoes.
SELECT COALESCE(MAX(inscricao), 0) AS maior_numero_atual
FROM participacao;
