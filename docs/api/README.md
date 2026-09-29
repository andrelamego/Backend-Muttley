# API e contratos HTTP

Os contratos estão nos controllers/DTOs e na [especificação OpenAPI versionada](openapi.yaml). Atualize o arquivo estático quando alterar entradas, respostas ou permissões. A descrição das regras está em [requisitos](../regras/requisitos-e-regras-de-negocio.md), e a comunicação com os serviços em [integrações](integracoes.md).

## Consultar a documentação em execução

Com a API na porta `8083`:

| Recurso | Endereço |
| --- | --- |
| Swagger UI | `http://localhost:8083/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8083/v3/api-docs` |
| OpenAPI YAML | `http://localhost:8083/v3/api-docs.yaml` |
| Disponibilidade da API | `http://localhost:8083/api/inicio` |

O arquivo estático permite consultar contratos sem iniciar a aplicação. A documentação em execução é gerada pelas anotações do código; não use um export antigo como prova de um contrato alterado.

## Autenticação

1. Envie `POST /api/auth/login` com `{email, senha}`.
2. Use o `accessToken` recebido no cabeçalho `Authorization: Bearer <token>`.
3. No Swagger, use **Authorize** e informe somente o token no esquema `bearerAuth`.

USER pode acessar os próprios dados e participações. ADMIN pode acessar `/api/admin/**` e também sua área pessoal. As sessões são stateless e o JWT tem duração padrão de duas horas.

O cadastro público acontece em duas etapas: solicitação sem senha e conclusão por convite. Veja [cadastro](../regras/cadastro.md) para os contratos, validade do token e bootstrap do primeiro ADMIN.

## Grupos de endpoints

| Grupo | Base e finalidade |
| --- | --- |
| Sistema | `/api/inicio`: disponibilidade |
| Autenticação | `/api/auth`: solicitação de cadastro, conclusão e login |
| Perfil | `/api/me`: dados, participações, certificados e medalhas do titular |
| Eventos públicos | `/api/eventos`: consulta, inscrição e confirmação de presença |
| Certificados públicos | `/api/certificados/{codigo}`: consulta, preview e download |
| Administração | `/api/admin`: dashboard, eventos, pessoas, certificados e cadastros auxiliares |
| Participações | `/api/participacoes`: gestão restrita ao titular ou ADMIN |

Uploads de conclusão e de assinatura usam `multipart/form-data`; consulte os nomes e formatos dos campos no OpenAPI/controllers antes de montar a requisição.

## Erros

Erros podem usar `erro`, `erros` ou `message`, conforme a origem. O cliente deve preservar status e mensagens: `400` para entrada inválida, `401` para autenticação, `403` para acesso negado, `404` para recurso/convite indisponível e `409` para conflito de estado. O download de QR usa `503` quando o microsserviço está indisponível.
