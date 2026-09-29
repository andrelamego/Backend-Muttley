# Muttley — backend

API de gestão de eventos acadêmicos, participações, certificados e medalhas. Usa Java 21, Spring Boot, JPA, MariaDB/MySQL e Kafka. PDF, QR Code e envio de e-mail são serviços separados.

## Começar

Para iniciar API, banco, Kafka e microsserviços juntos, siga o [README da infraestrutura](../Infra-Muttley/README.md). O frontend é executado separadamente em [front-muttley](../front-muttley/README.md).

Para executar somente a API, configure o [ambiente local](docs/operacao/configuracao-backend.md) e rode, na raiz deste repositório:

```powershell
mvn spring-boot:run
```

A API usa a porta `8083`. `GET /api/inicio` permite conferir sua disponibilidade. O [guia da API](docs/api/README.md) apresenta os contratos e a autenticação.

## Documentação

O [índice de documentação](docs/README.md) organiza as referências por assunto:

| Assunto | Referência |
| --- | --- |
| Requisitos e regras de negócio | [Regras do sistema](docs/regras/requisitos-e-regras-de-negocio.md) |
| Cadastro por convite e administrador inicial | [Cadastro](docs/regras/cadastro.md) |
| Contratos HTTP e microsserviços | [API](docs/api/README.md) e [integrações](docs/api/integracoes.md) |
| Configuração, dados e execução | [Operação](docs/operacao/README.md) e [migrações](docs/operacao/migracoes.md) |
| Ambiente com dados fictícios | [Massa completa para testes](docs/operacao/massa-de-testes.md) |
| Testes e rastreabilidade | [Testes](docs/testes/README.md) |
| Diagramas do projeto | [Diagramas](docs/diagramas/README.md) |

## Verificar alterações

```powershell
mvn test
# Integração com banco descartável; requer Docker ativo.
mvn -Pintegration verify
```

Os comandos, as fronteiras simuladas e os limites das suítes estão no guia de testes. Relatórios de execução ficam em `target/`, fora do Git.
