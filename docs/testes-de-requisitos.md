# Muttley — testes dos requisitos e das regras de negócio

> Registro histórico da primeira suíte, antes das regras esclarecidas pelo autor. Os números e as pendências abaixo descrevem aquela execução. Consulte o [relatório atualizado dos testes prioritários](./testes-prioritarios-2026-09-03.md) para o estado atual.

Análise e execução em **03/09/2026**. Referência: [requisitos e regras de negócio](./requisitos-e-regras-de-negocio.md).

Foram implementados e executados **228 casos de teste, com zero falhas, zero erros e zero casos ignorados**, distribuídos em cinco módulos. A suíte combina testes unitários, validação de DTOs, alguns testes HTTP isolados e processamento local do template de certificado. Ela não inicia MySQL, Kafka, Gmail nem Chromium.

**Esse resultado não significa que todos os requisitos do sistema estejam integralmente garantidos.** A [matriz de rastreabilidade](./matriz-requisitos-testes.csv) relaciona cada um dos **89 identificadores RF/RN** aos testes pertinentes e explicita seus limites. Um requisito aparecer na matriz significa que foi analisado, não que recebeu cobertura completa de ponta a ponta.

## Resultado executado

| Módulo | Casos executados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| Backend-Muttley | 186 | 0 | 0 | 0 |
| Microservice-Email-Muttley | 10 | 0 | 0 | 0 |
| Microservice-Pdf-Muttley | 6 | 0 | 0 | 0 |
| Microservice-QrCode-Muttley | 6 | 0 | 0 | 0 |
| front-muttley | 20 | 0 | 0 | 0 |
| **Total** | **228** | **0** | **0** | **0** |

Casos parametrizados são contados individualmente. Não foi medido percentual de cobertura de linhas ou branches: a quantidade de testes não é uma medida de cobertura de código.

O resumo desta execução está em [evidencias-testes/resultado-2026-09-03.json](./evidencias-testes/resultado-2026-09-03.json). Os logs completos regeneráveis ficam em `Backend-Muttley/target/relatorios-requisitos/`; os relatórios XML do JUnit ficam em `target/surefire-reports/` de cada serviço.

## Correções demonstradas pelos testes

Os testes foram inicialmente executados contra a implementação existente. A primeira rodada de 70 casos apresentou cinco falhas de asserção, referentes aos três primeiros problemas abaixo. Uma rodada posterior reproduziu o erro de validação de turno. Depois das correções, os mesmos cenários passaram.

| Problema observado | Correção | Teste de regressão |
|---|---|---|
| Inscrever uma pessoa já cadastrada como ADMIN alterava seu perfil para USER. | A inscrição só atribui USER quando a pessoa ainda não possui perfil. | `ParticipacaoServiceTest.RN_PAR_02_03_reutilizaPessoaSemSobrescreverCadastro`, por CPF e por email. |
| O mapper copiava o hash de senha para o DTO e a serialização permitia devolver a propriedade `senha`. | Mapper ignora a senha na saída; DTO aceita a propriedade apenas na entrada JSON. | `PessoaServiceTest` e resposta HTTP em `PessoaControllerTest`. |
| O parser de horário aceitava `24:00` como `00:00`. | Parse de `HH:mm` com resolução estrita. | `EventoServiceTest.RN_EVT_03_04_rejeitaHorarioInvalido`. |
| `@NotBlank` aplicado a um enum impedia validar disciplina, mesmo com turno válido. | O campo turno utiliza `@NotNull`. | `RequisitosDeEntradaTest`, para turno ausente e turno válido. |

Também foi introduzida uma configuração de diretório de assinatura **somente no fluxo de conclusão de evento**, `app.upload.assinaturas`, cujo padrão continua sendo `uploads/assinaturas`. Isso permite verificar a gravação com `@TempDir`, sem gravar arquivos de teste na pasta de assinaturas utilizada pelo sistema. Os outros endpoints de upload ainda usam o caminho fixo existente.

## O que os testes verificam

### Autenticação, pessoas e autorização

- Cadastro, escolha de perfil conforme a existência de administrador, email já encontrado, login válido e inválido, conclusão de cadastro parcial e rejeição de conclusão repetida.
- BCrypt real, preservação do hash quando a atualização não troca senha e geração de novo hash quando troca.
- Ausência de senha no mapper, na serialização e na resposta HTTP de consulta de pessoa.
- Assinatura e decodificação JWT reais, claims, intervalo de duas horas, rejeição por assinatura de outra chave e token expirado.
- Cadeia real de segurança com MockMvc: acesso público, falta de token, token malformado, USER proibido de acessar administração e ADMIN autorizado.
- Consultas pessoais utilizam a pessoa encontrada pelo subject do token; não escolhem arbitrariamente outro ID.
- CRUD de pessoas, listagens por perfil e preservação de múltiplos perfis durante a atualização.
- No frontend, normalização de perfis, compatibilidade com os claims existentes e comportamento diante de token ausente ou malformado. A leitura do token no navegador não valida assinatura e não substitui a autorização do backend.

Os endpoints usados em `AutorizacaoHttpTest` são endpoints de prova: exercitam a configuração real dos filtros, mas não executam os controllers de negócio. Os testes diretos de controller, por sua vez, não executam automaticamente os filtros de segurança nem interceptadores de transação.

### Eventos, inscrições e presença

- Evento novo CRIADO, dados de edição, referências existentes, horários inválidos, transição para EM_ANDAMENTO e restrições de cancelamento/conclusão.
- Exclusão dos eventos iniciados da lista disponível para inscrição e parâmetros enviados à consulta administrativa.
- Consulta pública, dados de participação e as duas solicitações de QR Code.
- Pessoa nova parcial, identificação por CPF/email, reaproveitamento do cadastro, conflito entre pessoas distintas e inscrição duplicada.
- Número sequencial em execução isolada, tipo Participante e presença inicialmente falsa.
- Rejeição de inscrição em evento iniciado, cancelado ou finalizado; confirmação de presença apenas de pessoa inscrita e rejeição da repetição.
- Conclusão com assinatura temporária: ignora IDs de outro evento, considera presenças já confirmadas, inclui as novas, solicita certificados apenas para presentes e notifica usando somente os certificados retornados como novos.
- Ordem de cancelamento/notificação e de geração de certificados/conclusão/notificação.

### Certificados e medalhas

- Dados automáticos do certificado, UUID, nova tentativa diante de colisão, preservação dos links já existentes e preenchimento de registros legados incompletos.
- Certificado preexistente, IDs repetidos no lote e segunda chamada não provocam nova emissão no cenário sequencial simulado.
- Vinculação de assinatura individual e por evento na camada de serviço.
- Template Thymeleaf real, escape do nome do participante, assinatura em Base64, datas e cálculo de horas/minutos.
- Contratos de preview inline e download attachment, tipo de conteúdo, comprimento e bytes do retorno.
- URL de compartilhamento no LinkedIn com código e data.
- Bronze automático exige presença e ID; bronze já existente impede outra gravação. Concessão manual aceita BRONZE, PRATA e OURO, e edição preserva identidade e vínculo.

O teste do PDF verifica o HTML enviado e o contrato do renderizador com Playwright simulado. **Ele não comprova a aparência nem a legibilidade de um PDF produzido pelo Chromium.**

### Cadastros auxiliares, indicadores e mensagens

- Validator real para obrigatoriedade dos campos dos DTOs de endereço, local, disciplina, patrocinador e dos cinco perfis especializados; CPF e email do cadastro completo.
- Local exige endereço existente; disciplina aceita professor ausente e rejeita professor informado inexistente.
- Painel: janelas de contagem, variação percentual positiva, negativa e com denominador zero; normalização das barras e limites dos rankings.
- Cinco tipos de notificação: tópico, destinatário, dados e conteúdo. O consumidor de email é exercitado também com o número de inscrição inteiro enviado pelo backend.
- QR Code: destinos de inscrição/presença, codificação da URL, tipo/chave da requisição, resposta de sucesso/erro e ausência de alteração do evento em resposta ERROR.
- PDF: HTML recebido, configuração A4, impressão de fundo, margens e liberação da página mesmo quando ocorre uma falha de renderização.

## Regras ainda não garantidas pela suíte

| Área | Limite ou divergência observada | Validação necessária |
|---|---|---|
| Unicidade | Consultar antes de gravar não evita duas gravações concorrentes. Número de inscrição continua usando maior número + 1. Certificado/bronze automáticos não têm a proteção de unicidade correspondente no banco. | Testes com MySQL, concorrência, transações e revisão das constraints. |
| Primeiro administrador | O código usa “não existe ADMIN”; o texto diz “primeiro usuário cadastrado”. Pessoas parciais USER podem existir antes do primeiro cadastro completo. | Definir a interpretação com o orientador e testar o cenário escolhido. |
| Cadastro parcial | Os testes de conclusão verificam que não é repetida após senha preenchida; não comprovam que o solicitante controla o email da pessoa. | Definir e testar comprovação de posse/token de ativação. |
| Presença | Falta teste HTTP do CPF no path. O documento não estabelece janela de confirmação por horário/status do evento. | Teste HTTP da validação e decisão sobre janela de presença, antes de criar nova regra. |
| Emissão direta | O fluxo de conclusão filtra os presentes, mas `CertificadoService.gerarCertificadosParaParticipacoes` não verifica presença por conta própria. | Avaliar defesa da regra no serviço e teste negativo de emissão direta. |
| Atualização de evento | Serviço testado; controller ainda pode usar ID do corpo diferente do ID da URL. | Fixar qual ID prevalece e testar o contrato HTTP. |
| Obrigatoriedade numérica | `numero`, `capacidade` e `inscricao` usam tipos primitivos: ausência no JSON pode virar zero; `@NotNull` não distingue ausência de zero. | Decidir domínio permitido e testar binding de campos omitidos. |
| Respostas de erro | Há testes HTTP de 400 com `erros`, 404, 401 e 403, e testes diretos de conflitos 409. Não há garantia uniforme de 400/409: exceções de estado e de integridade ainda podem chegar como 500. | Testes HTTP por fluxo e tratamento de exceções coerente. |
| Uploads | Gravação da conclusão verificada; endpoints individual/lote, arquivo obrigatório via multipart, MIME, tamanho e nomes não estão integralmente testados. | Testes HTTP multipart e definição da política de arquivos. |
| Consultas | Mocks confirmam parâmetros e uso dos resultados, não o comportamento das queries. | Filtros, paginação, ordenação, agregações, joins, isolamento por usuário e cascatas com banco real. |
| Integrações | KafkaTemplate, Gmail, cliente PDF e navegador estão simulados; produção e consumo são testados localmente. | Broker/HTTP reais, falhas, repetição de mensagens, entrega e recuperação. |
| Frontend | Apenas utilitários de perfil foram testados. Interceptors, formulários, mensagens, navegação e telas não têm cobertura nesta suíte. | Testes de componentes e E2E; conferir especialmente `erros`/`erro` do backend versus `errors`/`error` esperados pelo interceptor. |

Os demais itens da seção 13 do documento de requisitos continuam sendo decisões pendentes, incluindo limite por capacidade, CPF único/validação na inscrição, edição de cancelados e acesso ao CRUD de participações. Não foram convertidos silenciosamente em regras novas.

## Como executar

Pré-requisitos: JDK 21, Maven disponível e Node.js 22.18+ ou 24+. Execução verificada com Maven 3.9.14 e Node.js 24.12.0. Os testes do frontend utilizam o executor nativo do Node e não adicionam dependências npm.

Na raiz que contém os cinco projetos:

```powershell
.\Backend-Muttley\scripts\testar-requisitos.ps1
```

Se Maven não estiver no PATH ou for necessário usar o cache já instalado:

```powershell
.\Backend-Muttley\scripts\testar-requisitos.ps1 `
  -MavenCommand 'C:\Program Files\Apache\Maven\apache-maven-3.9.14\bin\mvn.cmd' `
  -MavenRepository 'C:\Users\andre\.m2\repository' `
  -Offline
```

`-Offline` só funciona quando todas as dependências estão no cache. Em uma máquina nova, omita essa opção. O script executa `clean test` em cada módulo Java, executa os testes do frontend, salva logs/resumo e retorna código diferente de zero se algum módulo falhar ou não executar testes.

Também é possível executar `mvn clean test` dentro de cada serviço e `npm test` dentro de `front-muttley`. Os casos do backend usam JUnit, Mockito, AssertJ e dependências de teste já presentes no projeto. Os mappers MapStruct são reais, exceto onde a dependência inteira é explicitamente simulada.

### Testes de inicialização existentes

Os três testes anteriores `*ApplicationTests` foram preservados com os nomes `MuttleyApplicationIT`, `PdfApplicationIT` e `QrcodeApplicationIT`. O sufixo IT diferencia os testes que carregam a aplicação inteira, evitando acionar infraestrutura ao executar a suíte isolada.

Eles **não foram executados nesta entrega** e não entram na contagem de 228. Não foram desabilitados com `@Disabled`. Para executá-los, use somente um ambiente de integração dedicado, com as dependências e credenciais de teste necessárias:

```powershell
# Dentro do respectivo serviço, com o ambiente de integração configurado:
mvn '-Dtest=*ApplicationIT' test
```

Não foi configurado Failsafe nem execução automática desses testes na fase `verify`; o comando explícito acima é necessário. Esses smoke tests de contexto, sozinhos, também não substituem os cenários de integração descritos na matriz.

## Uso na pré-banca

É possível demonstrar um comando reproduzível, os 228 casos passando e quatro defeitos corrigidos a partir de testes. A matriz permite explicar quais regras têm evidências locais e quais ainda precisam de validação com infraestrutura e usuários. Para a versão final do TCC, o próximo resultado de qualidade deve ser o fluxo completo: inscrição → presença → conclusão → certificado público, incluindo reexecução e tentativas concorrentes.
