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

Todos os erros HTTP tratados pela API usam o mesmo JSON, inclusive os retornados antes dos controllers pela segurança:

```json
{
  "status": 400,
  "codigo": "VALIDACAO",
  "erro": "Dados inválidos.",
  "erros": ["email: Email inválido"],
  "caminho": "/api/auth/login"
}
```

`status` repete o código HTTP; `codigo` é estável para tratamento no cliente; `erro` traz a mensagem legível; `erros` contém problemas por campo na validação e é `[]` nos demais casos; `caminho` usa o padrão da rota quando disponível, para não repetir tokens de convite, e nunca inclui parâmetros de consulta. O cliente deve usar `erro` como mensagem principal e, quando `erros` não estiver vazio, exibir os detalhes dos campos. Respostas bem-sucedidas não mudam.

Os códigos principais são `VALIDACAO` e `REQUISICAO_INVALIDA` (400), `NAO_AUTENTICADO` (401), `ACESSO_NEGADO` (403), `NAO_ENCONTRADO` (404), `METODO_NAO_PERMITIDO` (405), `MIDIA_NAO_SUPORTADA` (415), `CONFLITO` (409), `SERVICO_INDISPONIVEL` (503) e `ERRO_INTERNO` (500). Mensagens de falhas internas e de serviços externos não expõem detalhes técnicos. O [schema OpenAPI](openapi.yaml) descreve a estrutura.
