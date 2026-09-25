# Documentação OpenAPI / Swagger - Muttley API

Este documento descreve a implementação, os pontos de acesso e as instruções para utilização da especificação **OpenAPI 3** e da interface interativa **Swagger UI** no backend do projeto Muttley.

---

## 1. Visão Geral

A documentação interativa da API foi integrada através da biblioteca `springdoc-openapi-starter-webmvc-ui` (v2.8.5), com suporte a:
- **Catálogo de Endpoints**: Consulta detalhada de parâmetros, corpos de requisição e respostas.
- **Autenticação JWT Bearer**: Configuração do esquema de segurança `bearerAuth` permitindo autorizar e testar requisições protegidas diretamente pelo navegador.
- **Contratos Dinâmicos e Estáticos**: Geração de esquemas em tempo de execução (`/v3/api-docs`) e arquivo estático versionado para leitura e integração em [`docs/openapi.yaml`](./openapi.yaml).

---

## 2. Endpoints da Documentação

Com o backend em execução (porta padrão `8083`):

| Recurso | URL | Descrição |
|---|---|---|
| **Swagger UI (Interface Gráfica)** | `http://localhost:8083/swagger-ui.html` ou `/swagger-ui/index.html` | Interface web interativa para exploração e testes de endpoints |
| **OpenAPI Schema (JSON)** | `http://localhost:8083/v3/api-docs` | Especificação completa da API em formato JSON |
| **OpenAPI Schema (YAML)** | `http://localhost:8083/v3/api-docs.yaml` | Especificação completa da API em formato YAML |
| **Swagger UI Config** | `http://localhost:8083/v3/api-docs/swagger-config` | Configurações internas do Swagger UI |

---

## 3. Como Utilizar a Autenticação no Swagger UI

A maioria das operações administrativas e de perfil do Muttley exige um token JWT válido com o papel correspondente (`USER` ou `ADMIN`).

1. Acesse o Swagger UI em `http://localhost:8083/swagger-ui.html`.
2. No grupo **Autenticação**, utilize o endpoint `POST /api/auth/login`.
3. Preencha o email e senha no corpo da requisição e clique em **Execute**.
4. Copie o valor de `accessToken` retornado na resposta (ex: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`).
5. No topo da página do Swagger UI, clique no botão verde **Authorize** (com ícone de cadeado).
6. No campo `Value`, insira o token (não é necessário digitar `Bearer `, o Swagger aplica o prefixo automaticamente conforme a RFC 6750).
7. Clique em **Authorize** e feche a janela modal.
8. A partir desse momento, todas as requisições aos endpoints protegidos (`/api/me/**`, `/api/admin/**`, `/api/participacoes/**`) incluirão automaticamente o cabeçalho `Authorization: Bearer <seu-token>`.

---

## 4. Estrutura dos Módulos (Tags)

A API do Muttley está categorizada nos seguintes grupos de negócio:

1. **Sistema**: Verificação de disponibilidade e status inicial da API (`GET /api/inicio`).
2. **Autenticação**: Registro de usuários, conclusão de cadastro e emissão de tokens JWT (`/api/auth/**`).
3. **Meu Perfil**: Recursos restritos ao participante logado (`/api/me`, `/api/me/certificados`, `/api/me/medalhas`, `/api/me/participacoes`).
4. **Eventos**: Listagem e consulta pública de eventos, inscrições e confirmação presencial por CPF, além da gestão administrativa de eventos e QR Codes.
5. **Participações**: Gestão de inscrições e presenças dos participantes em eventos (`/api/participacoes/**`).
6. **Certificados Públicos**: Validação de autenticidade, pré-visualização inline e download em PDF de certificados emitidos, com link direto de compartilhamento no LinkedIn.
7. **Pessoas e Perfis**: Gestão de dados pessoais e atribuição de papéis (alunos, professores, palestrantes, organizadores, colaboradores).
8. **Administração - Dashboard**: Indicadores, gráficos de certificados emitidos e métricas gerenciais dos últimos 30 dias.
9. **Administração - Certificados**: Upload em lote e individual de assinaturas visuais de certificação.
10. **Administração - Disciplinas, Endereços, Locais, Medalhas e Patrocinadores**: CRUDs de cadastros auxiliares essenciais.

---

## 5. Especificação Estática

O arquivo [`docs/openapi.yaml`](./openapi.yaml) contém a especificação estática completa da API no padrão OpenAPI 3.0.3, podendo ser importado no **Postman**, **Insomnia**, **Swagger Editor** ou utilizado por ferramentas de geração de clientes frontend TypeScript/React.
