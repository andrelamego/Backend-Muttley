# Migrações do banco

O Flyway executa as migrações antes da inicialização do JPA e registra as versões em `flyway_schema_history`. O Hibernate usa `ddl-auto=validate`: confere os mapeamentos, sem criar ou alterar tabelas.

## Schema inicial

[`V1__estrutura_inicial.sql`](../../src/main/resources/db/migration/V1__estrutura_inicial.sql) cria as 15 tabelas atuais, as chaves estrangeiras, os índices únicos, os campos de convite e de assinatura, a garantia de uma participação por pessoa/evento e a sequência de inscrições. Usa InnoDB e `utf8mb4` em MySQL/MariaDB. Não cadastra eventos ou usuários; o administrador inicial continua sendo criado pelo [bootstrap](../regras/cadastro.md).

O primeiro início exige um schema vazio. A adoção de bancos anteriores não faz parte desta entrega. `baseline-on-migrate=false` impede que uma base preenchida sem histórico seja considerada migrada automaticamente. O comando `clean` do Flyway fica desabilitado na aplicação.

## Reiniciar a base local anterior

Como os dados anteriores foram descartados para esta etapa do desenvolvimento, recrie a base antes do primeiro início com Flyway. Para o [Compose](https://github.com/andrelamego/Infra-Muttley), execute na pasta `Infra-Muttley`:

```powershell
docker compose down
# Apaga o banco local anterior. O volume tem este nome no Compose do projeto.
docker volume rm muttley_mariadb_data
docker compose up --build -d
docker compose logs api
```

Esse procedimento apaga todos os dados do banco, incluindo usuários. O Compose mantém os volumes de assinaturas e Mailpit; o bootstrap configurado cria o administrador na base nova. Fora do Docker, recrie o schema local pelo seu cliente SQL e use a [configuração do backend](configuracao-backend.md).

Depois da inicialização, confira:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

Deve existir uma migração de versão `1` aplicada com sucesso. Novos inícios não reaplicam essa versão.

## Evoluir durante o desenvolvimento

1. Crie um novo SQL em `src/main/resources/db/migration/`, por exemplo `V2__adicionar_campo_evento.sql`, seguido de `V3`, `V4` etc. Use uma versão única e o separador `__`.
2. Faça a alteração correspondente nas entidades. O Flyway atualiza o schema antes da validação do Hibernate.
3. Não edite nem renomeie uma migração já aplicada. O Flyway verifica os checksums; uma correção deve ter uma versão nova.
4. Rode `mvn -Pintegration verify`. Os testes existentes criam bancos descartáveis pelo Flyway e validam o JPA. Acrescente testes de atualização somente quando uma alteração real exigir verificar conversão ou preservação de dados.

Não inclua dados de demonstração ou credenciais nas migrações de estrutura. Uma falha de migração ou de validação impede o início da API. MySQL/MariaDB podem fazer commit implícito de DDL; inspecione qualquer aplicação parcial antes de corrigir uma falha. Não use `repair`, alteração manual do histórico ou `ddl-auto=update` para ignorar divergências.

Referências oficiais: [integração do Flyway no Spring Boot 4](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) e [baseline do Flyway](https://documentation.red-gate.com/flyway/reference/commands/baseline).
