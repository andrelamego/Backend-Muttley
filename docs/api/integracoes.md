# Integrações

## Serviços e endereços

| Serviço | Repositório | Configuração da API | Endereço interno no Compose |
| --- | --- | --- | --- |
| PDF | [Microservice-Pdf-Muttley](https://github.com/andrelamego/Microservice-Pdf-Muttley) | `pdf.ms-url` | `http://pdf:8084` |
| QR Code | [Microservice-QrCode-Muttley](https://github.com/andrelamego/Microservice-QrCode-Muttley) | `qrcode.ms-url` | `http://qrcode:8086` |
| Kafka | [Infra-Muttley](https://github.com/andrelamego/Infra-Muttley) | `spring.kafka.bootstrap-servers` | `kafka:29092` |
| E-mail | [Microservice-Email-Muttley](https://github.com/andrelamego/Microservice-Email-Muttley) | Consumidor dos tópicos de notificação | Serviço `email`; SMTP para `mailpit:1025` |

Os valores locais e as variáveis de ambiente estão na [configuração](../operacao/configuracao-backend.md). O [Compose](https://github.com/andrelamego/Infra-Muttley) fornece os endereços entre containers e disponibiliza o Mailpit para consultar mensagens de teste.

## E-mail e Kafka

| Acontecimento | Tópico |
| --- | --- |
| Inscrição confirmada | `email.inscricao.confirmada` |
| Convite para completar cadastro | `email.completar.cadastro` |
| Evento cancelado | `email.evento.cancelado` |
| Evento concluído | `email.evento.concluido` |
| Certificado emitido | `email.certificado` |

No Compose, o microsserviço de e-mail usa SMTP e o Mailpit, sem enviar mensagens para caixas reais. Fora desse ambiente, o provedor padrão é Gmail e depende de sua configuração OAuth.

Notificações de conclusão/cancelamento e pedidos de QR são solicitados após o commit da transação. Isso evita publicar efeitos de uma transação revertida, mas ainda não garante recuperação entre o commit no banco e a publicação no Kafka. Outbox, deduplicação e recuperação após interrupção permanecem como trabalho operacional.

## QR Codes

- `GET /api/admin/eventos/{id}/qrcode-inscricao` e `qrcode-confirmacao` retornam PNG e exigem ADMIN.
- O backend chama `POST /api/qrcode/gerar` com `{eventoId, baseUrl, tema, tipo}`. `tipo` é `INSCRICAO` ou `CONFIRMACAO`.
- A geração síncrona usa ZXing no microsserviço e não depende de URLs previamente persistidas ou de QuickChart.
- Evento inexistente retorna `404`; indisponibilidade do serviço retorna `503`. O cliente limita conexão a 3 segundos e leitura a 10 segundos.
- Os destinos são `/eventos/{id}` e `/eventos/{id}/confirmar-presenca`, a partir de `app.frontend.url`. Para ler o código em um celular, essa URL precisa ser alcançável pelo aparelho.
- O fluxo Kafka de compatibilidade usa `qrcode.gerar.request` e `qrcode.gerar.response`; respostas bem-sucedidas atualizam a URL correspondente sob bloqueio do evento.

Gerar um QR Code não autoriza inscrição ou presença fora das regras do servidor.

## PDF e assinaturas

O backend prepara o HTML e envia `POST /api/pdf/gerar` com `htmlContent`. O serviço usa Chromium/Playwright para produzir o PDF. No Compose, sua imagem inclui os browsers da mesma versão da dependência Java.

Assinaturas são armazenadas pelo backend em `app.upload.assinaturas`. O Compose persiste esse diretório em volume. JPG/JPEG e PNG devem ter MIME e conteúdo de imagem válidos; confira as regras de upload nos controllers e em `AssinaturaStorage`.

## Banco e dashboard

Consultas por data usam `LocalDate`, como as entidades. O dashboard retorna resumos de eventos, sem expor proxies JPA ou participações.

Driver e dialeto são resolvidos para o banco configurado. MariaDB anterior a 10.6 usa `MariaDBLegacyDialect` pelo resolver do projeto; versões recentes e MySQL usam a resolução padrão. Bloqueios pessimistas preservam as regras de vagas e de conclusão concorrente.

As [suítes de testes](../testes/README.md) verificam contratos e concorrência; os testes do backend simulam os serviços externos e não comprovam sozinhos a cadeia completa de emissão e entrega.
