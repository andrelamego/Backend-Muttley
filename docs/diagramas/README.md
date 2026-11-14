# Diagramas do Muttley

As fontes Mermaid (`.mmd`) são editáveis. Os PNGs são renderizados diretamente dessas fontes, com fundo branco e escala 2 para leitura e uso em documentos. Os três diagramas antigos foram atualizados conforme o modelo atual; seus nomes de arquivo foram preservados para manter os links existentes.

| Diagrama | Mermaid | PNG | Escopo |
| --- | --- | --- | --- |
| Casos de uso | [Fonte](Muttley-Use-Case.mmd) | [Imagem](Muttley-Use-Case.png) | Ações públicas, área pessoal e administração |
| Classes | [Fonte](Muttley-Class-Diagram.mmd) | [Imagem](Muttley-Class-Diagram.png) | Entidades persistidas, atributos e associações |
| Entidades e relacionamentos | [Fonte](erd.mmd) | [Imagem](erd.png) | As 15 tabelas, chaves e cardinalidades da migração inicial |
| Visão arquitetural | [Fonte](visao-arquitetural.mmd) | [Imagem](visao-arquitetural.png) | Frontend, API, banco, arquivos, serviços e comunicação HTTP/Kafka/SMTP |

Para inserir no TCC, há também uma [visão arquitetural simplificada](documento/README.md), com menos detalhes e em uma pasta separada.

## Fontes e interpretação

Revisão baseada no código e na documentação local em 7 de outubro de 2026:

- [Requisitos e regras de negócio](../regras/requisitos-e-regras-de-negocio.md), [cadastro por convite](../regras/cadastro.md) e [integrações](../api/integracoes.md).
- [Entidades Java](../../src/main/java/com/fatec/muttley) e [controle de acesso](../../src/main/java/com/fatec/muttley/security/SecurityConfig.java).
- [Migração V1](../../src/main/resources/db/migration/V1__estrutura_inicial.sql), usada como referência do modelo físico.
- Clientes HTTP, produtores e consumidores de mensagens da API e dos serviços [QR Code](https://github.com/andrelamego/Microservice-QrCode-Muttley), [PDF](https://github.com/andrelamego/Microservice-Pdf-Muttley) e [e-mail](https://github.com/andrelamego/Microservice-Email-Muttley).

### Casos de uso

A representação usa `flowchart` com casos de uso em formas ovais, compatível com a versão do Mermaid utilizada neste diretório. É uma visão funcional, sem pretensão de representar todas as relações UML de inclusão/extensão.

Qualquer pessoa pode executar as ações públicas. A área pessoal atende tanto USER quanto ADMIN; as ações administrativas exigem ADMIN. Aluno, professor, palestrante, organizador e colaborador são perfis de domínio associados a uma pessoa, não roles adicionais de autorização. Compartilhamento com LinkedIn e o antigo ator “Coordenador de Curso” não foram mantidos como funcionalidades/permissões implementadas.

### Classes e banco

- O diagrama de classes mostra as entidades, atributos escalares e alguns métodos de domínio. Associações substituem os atributos de referência e coleção para evitar duplicação visual; DTOs, serviços, controllers e métodos gerados pelo Lombok foram omitidos.
- Os perfis especializados usam `@MapsId`: cada perfil compartilha a identidade de uma pessoa. São associações, não herança Java. Uma pessoa pode acumular perfis.
- As multiplicidades refletem os vínculos persistidos. Algumas FKs aceitam `NULL` no schema mesmo quando o fluxo da aplicação exige a associação. Por isso, por exemplo, local/disciplina/patrocinador aparecem como `0..1` em relação a cada evento.
- A FK única `certificado.id_participacao` limita a um certificado por participação. Embora a entidade use `@ManyToOne`, a unicidade restringe a cardinalidade efetiva a `0..1`.
- Em `participacao`, a unicidade é do par `(id_evento, id_pessoa)`. Cada coluna isolada continua aceitando repetições.
- `medalha.participacao_presenca_id` é uma coluna única para impedir bronze automático duplicado; não possui FK na V1. A relação com a participação usa `id_participacao`.
- `sequencia_inscricao` é uma tabela técnica independente para controle da numeração; não possui FK.
- No ERD, `PK`, `FK` e `UK` indicam chave primária, estrangeira e única. Linhas contínuas ligam os perfis cuja PK também é FK; linhas pontilhadas representam as demais associações com identidades próprias. O diagrama abrevia comprimentos dos tipos; o DDL é a fonte exata desses detalhes.

### Visão arquitetural

Linhas contínuas representam comunicação síncrona ou acesso ao armazenamento. Linhas tracejadas representam publicação/consumo de mensagens. Cada caixa dentro de Kafka é um tópico ou grupo de tópicos, não outro microsserviço.

| Fluxo | Implementação |
| --- | --- |
| Frontend ↔ API | HTTP/JSON; JWT nas rotas protegidas |
| API ↔ PDF | `POST /api/pdf/gerar`: HTML na solicitação e PDF na resposta |
| API ↔ QR Code | `POST /api/qrcode/gerar`: geração imediata de PNG com ZXing |
| API → Kafka → QR Code | Solicitação assíncrona em `qrcode.gerar.request` |
| QR Code → Kafka → API | Resultado em `qrcode.gerar.response`; a API atualiza a URL do evento |
| API → Kafka → e-mail | Confirmação de inscrição, convite de cadastro, cancelamento, conclusão e certificado |
| E-mail → SMTP | Mailpit no Compose local; provedor SMTP configurado para entrega externa |

O fluxo Kafka de QR Code é mantido por compatibilidade e atualmente devolve uma URL QuickChart montada pelo serviço; o PNG síncrono é produzido localmente com ZXing. Montar a URL não implica uma chamada HTTP ao QuickChart durante o consumo da mensagem. O diagrama mostra os dois caminhos e não atribui ao serviço PDF uma integração Kafka inexistente.

As publicações da API são adiadas até o commit quando existe sincronização transacional; fora de transação, o envio é imediato. Isso não equivale a uma outbox nem garante recuperação entre o commit e a publicação. O serviço de e-mail usa SMTP por padrão conforme sua configuração atual; a menção antiga a Gmail como padrão no documento de integrações ainda precisa ser harmonizada.

## Gerar novamente os PNGs

Requisitos: Node.js e npm. As versões das dependências estão fixadas pelo `package-lock.json`. O renderizador usa o [Mermaid CLI](https://github.com/mermaid-js/mermaid-cli).

A partir da raiz do backend:

```powershell
cd docs/diagramas
npm ci
npm run render
```

A instalação padrão do Puppeteer obtém o navegador necessário. Para utilizar um Chrome/Chromium já instalado, configure o caminho do executável:

```powershell
$env:PUPPETEER_SKIP_DOWNLOAD = 'true'
npm ci
$env:PUPPETEER_EXECUTABLE_PATH = 'C:/caminho/para/chrome.exe'
npm run render
```

Edite os `.mmd`, execute a renderização e versione as fontes e os PNGs juntos. A configuração compartilhada de cores e fontes está em [mermaid-config.json](mermaid-config.json). Mudanças nas entidades, nas migrações, nas permissões ou nas integrações exigem revisão dos diagramas correspondentes.
