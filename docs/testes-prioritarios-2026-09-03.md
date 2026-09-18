# Muttley — testes prioritários para a pré-banca

Execução em **03/09/2026**, seguindo o [guia de testes](./guia-de-testes.md) e as seis decisões esclarecidas pelo autor. Foram acrescentados testes de API e persistência com MySQL real, concorrência, validação de arquivos e regressões das regras de acesso.

## Regras adotadas

| Decisão | Comportamento implementado e testado |
|---|---|
| Inscrição até o início | Permitida antes do horário inicial. No instante exato do início, novas inscrições retornam `400`. |
| Presença com tolerância | Para evento das 10h às 11h: confirmação entre **9h50 e 11h10**, incluindo ambos os limites. Exige inscrição; cancelado ou finalizado não aceita confirmação. |
| Vagas esgotadas | A capacidade do local limita as participações do evento. Uma última vaga tem apenas um vencedor entre solicitações simultâneas, inclusive no CRUD autenticado. Lotação retorna `409`. |
| Participações próprias | USER só administra suas participações. IDs alheios na URL ou no corpo não concedem acesso. ADMIN administra as demais. |
| Assinatura JPG/PNG | Aceita `.jpg`, `.jpeg` e `.png`, com MIME e imagem decodificável coerentes. Arquivo vazio, texto disfarçado, GIF e formatos divergentes retornam `400`. |
| Email verificado | Continua como implementação futura; o fluxo atual permite completar cadastro e autenticar sem essa etapa. |
| Evento cancelado e identidade | Cancelado não pode ser editado (`409`). A atualização de evento permitido usa o ID da URL, preservando sua identidade mesmo que o corpo informe outro ID. |

## Resultado

| Suíte | Casos | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| Backend: unitários, validação, HTTP isolado e template | 195 | 0 | 0 | 0 |
| Backend: integração de API, MySQL e concorrência | 41 | 0 | 0 | 0 |
| Microsserviço de email: testes isolados | 10 | 0 | 0 | 0 |
| Microsserviço de PDF: testes isolados | 6 | 0 | 0 | 0 |
| Microsserviço de QR Code: testes isolados | 6 | 0 | 0 | 0 |
| Frontend: utilitários e serviços isolados | 20 | 0 | 0 | 0 |
| **Total** | **278** | **0** | **0** | **0** |

A suíte anterior possuía 228 casos. Esta etapa acrescenta **41 casos integrados e 9 unitários de assinatura**, além de ajustar testes existentes às regras definidas. Casos parametrizados são contados individualmente pelos executores.

Evidência consolidada: [resultado em JSON](./evidencias-testes/resultado-prioritarios-2026-09-03.json). A [matriz de rastreabilidade](./matriz-requisitos-testes.csv) contém os **93 identificadores RF/RN atuais**, com testes associados e limites. A presença de um requisito na matriz não significa cobertura integral; não foi medido percentual de cobertura de código.

## Cenários prioritários

Os testes estão em [`RegrasCriticasIT`](../src/test/java/com/fatec/muttley/integration/RegrasCriticasIT.java), com validações locais adicionais em [`AssinaturaStorageTest`](../src/test/java/com/fatec/muttley/certificado/AssinaturaStorageTest.java).

- Cadastro completo, primeiro ADMIN, demais USER, email duplicado, login real com BCrypt e JWT usado em `/api/me`.
- Inscrição pública criando cadastro parcial, conclusão do cadastro uma única vez e autenticação posterior.
- Limites temporais exatos de inscrição e presença, CPF inválido na confirmação, pessoa não inscrita e evento encerrado.
- Última vaga, pessoa duplicada, números de inscrição em eventos diferentes, presença, conclusão, certificados e bronze sob duas solicitações concorrentes.
- USER tentando listar, consultar, criar, atualizar, transferir e excluir participações de terceiros; ADMIN acessando participação alheia; consultas da área pessoal com duas pessoas no banco.
- Conclusão que ignora IDs de outro evento, emite apenas para presentes, mantém um certificado e um bronze automático e rejeita repetição.
- Falha provocada na emissão durante a conclusão: estado, presença e medalhas revertidos; arquivo novo removido; nenhuma publicação de email solicitada.
- Restrições reais do MySQL rejeitando email duplicado, participação repetida no evento e segundo certificado para a participação, inclusive por persistência direta.
- Assinaturas reais PNG/JPEG nas três rotas de upload e conteúdo incorporado ao template do certificado com MIME correto.
- POST de cadastro com ID alheio sem sobrescrever a pessoa existente; transferência de presença para outro evento bloqueada.

## Correções necessárias para atender aos cenários

1. **Tempo controlável:** `Clock` injetável nos fluxos de evento, inscrição, presença e emissão automática; tolerância de dez minutos em cada extremidade.
2. **Vagas e duplicidade:** bloqueio do evento durante inscrições; validação de capacidade também na criação e transferência autenticadas; chave única evento/pessoa. O CRUD de participações usa `READ_COMMITTED` para consultar vagas atuais após aguardar o bloqueio.
3. **Número de inscrição:** reserva numa sequência transacional compartilhada entre eventos. Uma transação revertida pode deixar lacunas; números reservados não são reutilizados. O campo ainda pode ser editado pelo CRUD e não possui constraint única própria.
4. **Autorização por proprietário:** verificação do usuário autenticado na gestão de participações e proteção contra reaproveitamento de ID em POST de cadastro.
5. **Conclusão e prêmios:** transações e bloqueios impedem repetição dos cenários testados. Certificado possui unicidade por participação; bronze automático possui marcador único, preservando medalhas manuais adicionais.
6. **Assinatura:** armazenamento compartilhado, nome aleatório, validação de conteúdo e limpeza do arquivo novo em rollback. Preview identifica JPEG corretamente.
7. **Notificações:** mensagens de conclusão/cancelamento são solicitadas após commit. Isso evita notificar uma conclusão revertida; ainda não garante entrega entre banco e Kafka após queda do processo.
8. **Ambiente de testes:** profile `test` sem `MockDataInitializer`, MySQL descartável, limpeza entre cenários e diretório temporário de assinaturas.

## Como executar novamente

Pré-requisitos usados: **Java 21, Maven 3.9.14, Node 24.12.0 e Docker em execução**. Na primeira execução, Maven precisa baixar dependências e o Docker precisa obter as imagens de MySQL 8.4 e suporte do Testcontainers. Não é necessário preparar banco manualmente nem configurar Gmail.

A partir de `Backend-Muttley`:

```powershell
# Todos os módulos, incluindo integração MySQL do backend
./scripts/testar-requisitos.ps1 -Integration

# Somente backend: testes rápidos e integração
mvn -Pintegration verify

# Todos os módulos, apenas testes rápidos
./scripts/testar-requisitos.ps1
```

O script aceita `-MavenCommand`, `-MavenRepository` e `-Offline` quando as dependências já estão em cache. Sem Docker disponível, o perfil de integração falha: os testes não são silenciosamente ignorados.

Relatórios regeneráveis: `target/surefire-reports`, `target/failsafe-reports` e `target/relatorios-requisitos` no backend, além de `target/surefire-reports` em cada microsserviço. O script consolida `resumo.json` e logs dos cinco módulos. Os testes `*ApplicationIT` históricos permanecem fora da seleção; o perfil executa `integration/*IT.java`.

O profile de testes usa `@ServiceConnection` para conectar somente ao MySQL criado pelo Testcontainers. A URL padrão do profile é uma porta local inválida, impedindo reutilização acidental da configuração normal. O banco de desenvolvimento não foi usado para estas execuções.

## Antes de usar o código com dados existentes

Foram adicionadas restrições únicas e estruturas de apoio no modelo. Os testes validam criação de esquema vazio; **não validam migração do banco existente**. Antes de atualizar esse banco, conferir duplicidades e preparar a migração do esquema. O arquivo [verificar-integridade-antes-da-atualizacao.sql](./sql/verificar-integridade-antes-da-atualizacao.sql) contém apenas consultas para essa conferência e não foi executado no banco de desenvolvimento. Nenhum dado existente é corrigido ou excluído automaticamente.

## Limites e próximos testes

- **Navegador:** cadastro, inscrição, presença e download percorrendo a interface ainda precisam de E2E. A apresentação de lotação no frontend também precisa dessa validação.
- **Serviços externos:** Kafka, Gmail, PdfClient e QrCodeClient estão simulados nos testes do backend. Não houve envio real de email, leitura de QR por câmera ou renderização de PDF pelo Chromium nesta etapa.
- **Concorrência:** os testes coordenam duas solicitações por cenário; não são ensaios de carga, nem cobrem todas as combinações de edição, transferência, cancelamento e exclusão simultâneas.
- **Regras futuras:** verificação de email/posse do cadastro, normalização e unicidade de CPF, política para eventos online e limites de tamanho/dimensões de assinatura ainda precisam de trabalho específico.
- **Operação:** migrações, recuperação entre commit e envio ao Kafka, mensagens repetidas, indisponibilidade dos serviços e falhas de disco após interrupção do processo permanecem fora desta suíte.

Para a pré-banca, este resultado fornece evidências reproduzíveis das regras mais críticas. Ele não representa garantia de que todos os requisitos funcionais e não funcionais já estão atendidos de ponta a ponta.
