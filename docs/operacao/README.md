# Operação e dados

| Tarefa | Referência |
| --- | --- |
| Subir o conjunto de serviços | [Docker Compose](../../../Infra-Muttley/README.md) |
| Configurar a API fora do Docker | [Configuração do backend](configuracao-backend.md) |
| Criar o administrador inicial | [Cadastro e bootstrap](../regras/cadastro.md) |
| Reprogramar eventos de demonstração | [Atualização de massa](atualizacao-massa-eventos.md) |
| Conferir duplicidades antes de atualizar o schema | [Consultas de integridade](sql/verificar-integridade-antes-da-atualizacao.sql) |

O Compose usa volumes persistentes para banco, assinaturas e mensagens de teste. A alteração da senha em `.env` não altera contas já existentes no volume; o README da infraestrutura explica esse caso e o healthcheck de autenticação.

Faça backup antes de alterar dados ou schema. A configuração atual usa `ddl-auto=update`; uma estratégia de migrações versionadas ainda deve ser definida para o lançamento. Os testes de integração criam bancos vazios e não comprovam a migração de uma base existente.
