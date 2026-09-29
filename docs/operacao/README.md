# Operação e dados

| Tarefa | Referência |
| --- | --- |
| Subir o conjunto de serviços | [Docker Compose](https://github.com/andrelamego/Infra-Muttley) |
| Configurar a API fora do Docker | [Configuração do backend](configuracao-backend.md) |
| Criar e evoluir o schema | [Migrações com Flyway](migracoes.md) |
| Criar o administrador inicial | [Cadastro e bootstrap](../regras/cadastro.md) |
| Popular o ambiente de testes | [Massa completa e cenários](massa-de-testes.md) |
| Reprogramar eventos de demonstração | [Atualização de massa](atualizacao-massa-eventos.md) |
| Diagnosticar duplicidades | [Consultas de integridade](sql/verificar-integridade-antes-da-atualizacao.sql) |
| Validar e publicar uma versão | [Release](release.md) |

O Compose usa volumes persistentes para banco, assinaturas e mensagens de teste. A alteração da senha em `.env` não altera contas já existentes no volume; o README da infraestrutura explica esse caso e o healthcheck de autenticação.

O Flyway cria e evolui o schema; o Hibernate apenas valida os mapeamentos. O primeiro início desta base exige um schema vazio, conforme o guia de migrações. Para futuras alterações sobre dados que devam ser mantidos, faça backup e valide as conversões necessárias antes da atualização.
