# Testes e rastreabilidade

As regras estão em [requisitos e regras de negócio](../regras/requisitos-e-regras-de-negocio.md). A [matriz RF/RN → testes](matriz-requisitos-testes.csv) relaciona identificadores, classes de teste e limites. A presença de um requisito na matriz não comprova cobertura integral nem cobertura percentual de código.

## Executar

Requisitos: Java 21 e Maven. Para integração, Docker deve estar ativo; para o script de todos os módulos, use Node.js 24 e mantenha os repositórios como pastas irmãs.

Na raiz de `Backend-Muttley`:

```powershell
# Testes rápidos do backend, sem banco de desenvolvimento.
mvn test

# Integração com MySQL 8.4 descartável, criado pelo Testcontainers.
mvn -Pintegration verify

# Integração com MariaDB 10.4.32 descartável.
mvn -Pintegration '-Dmuttley.test.database=mariadb' verify

# Backend, microsserviços e testes isolados do frontend.
./scripts/testar-requisitos.ps1

# Todos os módulos, incluindo integração MySQL do backend.
./scripts/testar-requisitos.ps1 -Integration
```

O script aceita `-MavenCommand`, `-MavenRepository` e `-Offline`. A integração não utiliza o banco do Compose nem a base de desenvolvimento. O profile `test` aponta por padrão para uma porta inválida; `@ServiceConnection` fornece a conexão do container descartável. O Flyway cria o schema e o Hibernate valida os mapeamentos, como na aplicação.

O perfil Maven seleciona `integration/*IT.java`. Testes históricos `*ApplicationIT` fora dessa pasta não fazem parte dessa seleção.

## Onde estão os resultados

| Executor | Saída regenerável |
| --- | --- |
| Maven/Surefire | `target/surefire-reports/` em cada serviço |
| Maven/Failsafe | `target/failsafe-reports/` no backend |
| Script de todos os módulos | `target/relatorios-requisitos/`, com logs e `resumo.json` |
| Frontend | Saída de `npm test` ou do script agregado |

Execute a suíte para obter os números atuais. Relatórios de execuções antigas, capturas e logs não são mantidos em `docs/`; `mvn clean` remove `target/`.

## Cobertura por responsabilidade

| Área | Testes principais | Fronteira verificada |
| --- | --- | --- |
| Cadastro e login | `AuthControllerTest`, `CadastroConviteServiceTest`, `BootstrapAdminConfigTest`, `RegrasCriticasIT` | USER público, convite de uso único, expiração, identidade vinculada, bootstrap, BCrypt e JWT |
| Autorização | `AutorizacaoHttpTest`, `MeControllerTest`, `PessoaControllerTest`, `RegrasCriticasIT` | ADMIN, titular, dados e participações de terceiros |
| Eventos e inscrições | `HorariosEventoTest`, `EventoServiceTest`, `EventoControllerTest`, `ParticipacaoServiceTest`, `RegrasCriticasIT` | Início, capacidade, duplicidade, janela de presença, cancelamento e ID preservado |
| Certificados e medalhas | `CertificadoServiceTest`, `CertificadoPublicoControllerTest`, `MedalhaServiceTest`, `RegrasCriticasIT` | Emissão única, presentes, bronze automático e rollback da conclusão |
| Assinaturas | `AssinaturaStorageTest`, `CertificadoAssinaturaVisualTest`, `RegrasCriticasIT` | PNG/JPEG, MIME, conteúdo decodificável e incorporação no template |
| Dashboard e QR | `AdminControllerTest`, `QrCodeClientTest`, `QrCodeMensageriaTest`, `RegrasCriticasIT` | Datas, resumos, publicação após commit, respostas concorrentes, destinos de QR e erros HTTP |
| Entradas e auxiliares | `RequisitosDeEntradaTest`, `ReferenciasDeCadastroTest`, `PessoaServiceTest` | Validação de dados e referências existentes |
| Contratos | `OpenApiTest`, testes dos microsserviços e do frontend | Especificação, adaptação de respostas e comportamentos isolados |
| Massa local | `MassaDadosConfigTest`, `MassaDadosIT` | Ativação explícita, dados coerentes, idempotência, contas e convites |

Os testes de concorrência coordenam solicitações em cenários de última vaga, inscrição duplicada, confirmação, emissão e conclusão. Não substituem ensaios de carga.

## Limites e próximos testes

- Kafka, e-mail, PDF e QR são simulados nas fronteiras dos testes integrados do backend. O Compose permite verificar a comunicação real, mas sua inicialização não comprova o fluxo completo.
- E2E deve percorrer solicitação/convite de cadastro, login, inscrição, presença, conclusão e download, pela interface e pelos serviços reais.
- Testes visuais devem conferir celular e desktop, teclado, estados de erro/vazio/carregamento e movimento reduzido.
- Conversões de dados em futuras migrações, outbox, redelivery de mensagens, indisponibilidade e recuperação após falha de disco/processo precisam de testes próprios conforme sua implementação.
- Consulte as [consultas de integridade](../operacao/sql/verificar-integridade-antes-da-atualizacao.sql) antes de preparar uma migração; elas não corrigem dados automaticamente.

## Manter os testes

Use `Clock` injetável para horários, fixtures isoladas e bancos descartáveis. Não use pessoas reais, e-mails reais ou a base de demonstração para executar as suítes automatizadas. A [massa completa](../operacao/massa-de-testes.md) é destinada às jornadas manuais e usa dados sintéticos. Preserve os IDs de requisitos na matriz, atualize descrições quando mudar uma regra e relate separadamente o que é simulado e o que foi validado com serviços reais.
