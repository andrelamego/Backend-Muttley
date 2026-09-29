# Massa completa para testes locais

A massa é sintética e opcional. O profile `massa` e `muttley.massa.enabled=true` precisam estar ativos juntos. O profile `test` bloqueia a carga automática. Nenhum dado demonstrativo é incluído nas migrações do Flyway.

## Conteúdo

| Recursos criados pela carga | Quantidade |
| --- | ---: |
| Pessoas | 72: 2 ADMIN e 70 USER, incluindo 3 cadastros pendentes |
| Alunos / professores / palestrantes | 48 / 8 / 6 |
| Organizadores / colaboradores | 5 / 4 |
| Endereços / locais | 6 / 7 |
| Disciplinas / patrocinadores | 12 / 8 |
| Eventos | 36: 14 CRIADO, 4 EM_ANDAMENTO, 12 FINALIZADO e 6 CANCELADO |
| Participações | 581 |
| Certificados | 147 |
| Medalhas | 209: 197 bronze, 6 prata e 6 ouro |

Os perfis especializados podem se sobrepor. Se o bootstrap já criou um administrador, ele é preservado e soma uma pessoa à contagem acima. Seu email e sua senha continuam sendo os configurados no ambiente.

Os emails usam `example.test`; nomes, instituições, endereços, empresas e assinaturas são fictícios. CPF e CNPJ são gerados com dígitos verificadores válidos para permitir exercitar as validações, sem associação intencional com pessoas ou empresas reais.

## Carregar pelo Compose

Requer a API com Flyway e uma base vazia, admitindo somente o administrador criado pelo bootstrap. Na pasta [Infra-Muttley](https://github.com/andrelamego/Infra-Muttley):

```powershell
docker compose -f docker-compose.yml -f docker-compose.massa.yml up --build -d
docker compose logs api
```

O arquivo adicional habilita a carga somente na API. Banco, Kafka, PDF, QR e email seguem a configuração local existente; mensagens novas são capturadas no Mailpit. A criação da massa não envia emails, não publica pedidos de QR e não solicita PDFs.

Para recriar a massa e atualizar suas datas a partir de hoje, apague a base local de testes e suba novamente:

```powershell
docker compose -f docker-compose.yml -f docker-compose.massa.yml down
# Apaga todos os usuários e dados deste banco local.
docker volume rm muttley_mariadb_data
docker compose -f docker-compose.yml -f docker-compose.massa.yml up --build -d
```

Para executar fora do Docker, configure os serviços e o banco conforme [configuração](configuracao-backend.md) e rode:

```powershell
mvn spring-boot:run '-Dspring-boot.run.arguments=--spring.profiles.active=massa --muttley.massa.enabled=true'
```

Use o fuso `America/Sao_Paulo`, também utilizado no Compose. Sem as duas opções de ativação, a aplicação não cria a massa. Para interromper a carga automática depois de criada, use o Compose sem o arquivo adicional; os dados continuam no volume.

## Contas

Senha das contas completas da massa: **`Muttley-Teste-2026!`**.

| Email | Perfil e cenário |
| --- | --- |
| `admin.massa@example.test` | ADMIN, também organizador; dashboard e gestão |
| `gestao.massa@example.test` | Segundo ADMIN para comparar sessões |
| `ana.massa@example.test` | USER/aluna, com eventos futuros, histórico, certificados e medalhas |
| `bruno.massa@example.test` | USER/aluno para comparar permissões e participações de titulares diferentes |
| `usuario03.massa@example.test` até `usuario64.massa@example.test` | Demais contas completas e perfis especializados |
| `vazio.massa@example.test` | USER sem participações, certificados ou medalhas |
| `novo.massa@example.test` | USER sem histórico para novas inscrições |
| `convite.consumido@example.test` | USER com cadastro concluído; convite antigo inválido |
| `convite.valido@example.test` | Cadastro parcial, sem senha, com convite válido por 24 horas |
| `convite.expirado@example.test` | Cadastro parcial, sem senha, com convite expirado |
| `sem.convite@example.test` | Cadastro parcial sem CPF, telefone, senha ou convite |

As contas parciais não fazem login antes da conclusão do cadastro. Os tokens conhecidos abaixo pertencem exclusivamente à massa local:

- Válido: `muttley-massa-convite-valido-2026`.
- Expirado: `muttley-massa-convite-expirado-2026`.
- Consumido: `muttley-massa-convite-consumido-2026` — não está mais associado a uma conta.

Use `GET /api/pessoa/dados-cadastro/{token}` e `PUT /api/auth/register?token=...` para testar o contrato. O CPF da conta vinculada pode ser consultado no cadastro administrativo. A conclusão precisa enviar nome, email e CPF correspondentes, telefone e senha. O frontend ainda precisa da jornada `/register`; os dados não implementam uma tela ausente.

Esses convites são fixtures conhecidas, sem publicação no Kafka. Para verificar a entrega real no Mailpit, solicite um cadastro com outro email de teste ou renove o convite expirado pelo endpoint público.

## Eventos e jornadas principais

| ID | Cenário inicial |
| --- | --- |
| `40001` | Futuro sem inscrições; iniciar cadastro e testar estados vazios |
| `40002` | Lotado: 6 de 6 vagas; nova inscrição deve ser recusada |
| `40003` | Última vaga: 5 de 6; útil para concorrência de inscrições |
| `40004` a `40012` | Catálogo futuro, datas variadas, modalidades, locais e turnos |
| `40013` | Próximo horário, cerca de 30 minutos após a carga; inscrição ainda aberta |
| `40014` | Início cerca de 5 minutos após a carga; presença dentro da tolerância anterior ao início |
| `40015` | Em andamento: 21 inscritos, 12 presentes; testar presença, conclusão e assinatura |
| `40016` | Em andamento: 12 inscritos, 6 presentes |
| `40017` e `40018` | Horário encerrado, aguardando conclusão; confirmação de presença fora da janela |
| `40019` a `40030` | Finalizados, presentes e ausentes; acervo de certificados e métricas em diferentes meses |
| `40031` a `40036` | Cancelados, com e sem inscrições; bloqueio de edição, presença e cancelamento repetido |

Os horários próximos à meia-noite são ajustados para que cada evento comece e termine na mesma data. As presenças já marcadas recebem bronze; os ausentes não recebem certificados. Medalhas manuais de prata e ouro complementam o histórico de Ana. A sequência é ajustada para a próxima inscrição continuar após `581`.

Há certificados com código público e PNG de assinatura claramente identificado como fictício, salvo em `app.upload.assinaturas/massa/assinatura-ficticia.png`. A consulta pública e a imagem não dependem de PDFs pré-gerados. Preview e download geram o PDF sob demanda e exigem o microsserviço; QR Codes também são gerados sob demanda pelo serviço de QR.

## Repetição e limites

A carga acontece em uma transação. Reiniciar uma base já carregada mantém registros, datas, convites e alterações manuais. As datas se referem ao momento da primeira carga e não são avançadas a cada reinício; use a recriação explícita para obter novos cenários em andamento. Uma base já utilizada sem o marcador da massa é recusada, sem apagar ou sobrescrever seus registros.

As quantidades descrevem a carga inicial. Confirmações, conclusões, novas inscrições e o bootstrap podem alterá-las. O conjunto cobre navegação, paginação, filtros, permissões, estados vazios e jornadas operacionais; não é uma massa de carga/performance nem substitui os testes automatizados.

Validação automatizada: `MassaDadosConfigTest` verifica a ativação e o isolamento; `MassaDadosIT` verifica integridade, capacidade, certificados, CPFs, idempotência, contas, convites e composição do PDF com fronteiras externas simuladas.
