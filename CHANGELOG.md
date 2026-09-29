# Changelog

As versões deste arquivo se referem ao backend. Infraestrutura, microsserviços e frontend possuem ciclos de entrega próprios.

## 0.1.0-alpha — 2026-09-29

Primeira versão alpha, destinada a avaliação e testes dos fluxos de eventos acadêmicos.

### Funcionalidades

- Autenticação JWT, perfis USER/ADMIN e consultas dos dados do usuário autenticado.
- Solicitação de cadastro, conclusão por convite com validade e criação do administrador inicial por configuração.
- Gestão de eventos, disciplinas, locais, endereços, pessoas, perfis especializados e patrocinadores.
- Inscrição até o início do evento, controle de vagas e proteção contra inscrições duplicadas ou concorrentes.
- Confirmação de presença com tolerância de dez minutos antes do início e após o término.
- Cancelamento e conclusão de eventos, emissão de certificados para presentes e medalhas de participação.
- Consulta pública de certificados, assinatura visual JPG/PNG e geração de PDF e QR Code por microsserviços.
- Dashboard administrativo com eventos, certificados e medalhas.

### Operação e testes

- Schema inicial e futuras evoluções pelo Flyway; Hibernate configurado para validar a estrutura.
- Execução local com banco, Kafka, microsserviços e Mailpit pelo [Infra-Muttley](https://github.com/andrelamego/Infra-Muttley).
- Massa opcional com 72 pessoas fictícias, 36 eventos, 581 participações, 147 certificados e 209 medalhas.
- Testes unitários e integração com bancos descartáveis para regras, permissões, concorrência e massa de dados.

### Limites desta alpha

- A primeira inicialização com Flyway exige um banco vazio; adoção de bases antigas não está incluída.
- A verificação de email como funcionalidade própria permanece planejada. O cadastro atual usa convite enviado por email.
- Notificações e pedidos de QR após commit ainda não têm outbox ou garantia de recuperação entre banco e Kafka.
- Assinaturas são imagens visuais; não constituem assinatura criptográfica ou certificação digital.
- Os testes integrados do backend simulam PDF, QR e email nas fronteiras. A jornada E2E completa e ensaios de carga continuam pendentes.
- Contratos e schema podem mudar durante a alpha. Confira as notas e migrações de cada atualização.

Consulte o [guia de release](docs/operacao/release.md) para preparar o artefato, conferir o ambiente e publicar a versão.
