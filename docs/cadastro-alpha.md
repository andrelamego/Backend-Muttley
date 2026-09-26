# Cadastro seguro para a alpha

O cadastro público não concede `ADMIN` e não recebe senha. `POST /api/auth/register` aceita `nome` e `email`, cria uma conta `USER` pendente e responde `202` sem indicar se o email já existe. CPF e telefone são informados somente após a pessoa abrir o convite recebido no próprio email. O link contém um token aleatório de uso único válido por 24 horas. O banco armazena apenas o SHA-256 do token. Solicitações repetidas durante a validade não substituem nem reenviam o convite; após a expiração, uma nova solicitação gera outro link.

O link continua no formato `/register?id=<token>` para manter a mensagem do microsserviço de email. O cliente deve usar o valor de `id` em `GET /api/pessoa/dados-cadastro/{token}` e em `PUT /api/auth/register?token=<token>`, enviando neste último os dados cadastrais e a senha. O email deve corresponder à conta vinculada ao convite; o CPF também deve corresponder quando já estiver registrado por uma inscrição em evento. Um convite consumido ou expirado retorna `404`.

Inscrições públicas em eventos continuam criando uma conta pendente quando necessário e enviando o mesmo tipo de convite. O envio efetivo depende do microsserviço de email; a correção da entrega e o tratamento de falhas desse serviço são um trabalho separado.

O administrador inicial **não** é criado por uma requisição pública. Para criá-lo em uma base nova, iniciar uma instância com as variáveis `MUTTLEY_BOOTSTRAP_ADMIN_ENABLED=true`, `MUTTLEY_BOOTSTRAP_ADMIN_EMAIL` e `MUTTLEY_BOOTSTRAP_ADMIN_PASSWORD` (mínimo de 12 caracteres). `MUTTLEY_BOOTSTRAP_ADMIN_NAME` é opcional. Depois do primeiro início bem-sucedido, remover essas variáveis do ambiente. O seed de demonstração `MockDataInitializer` só é carregado com o profile `dev`; não usar esse profile na alpha.

Antes de atualizar uma base existente, aplicar e verificar as colunas `cadastro_token_hash` (VARCHAR(64), único, anulável) e `cadastro_token_expira_em` (TIMESTAMP, anulável) na tabela `pessoa`. A estratégia de migrações versionadas ainda precisa ser definida antes do lançamento. Links antigos baseados em Hashids deixam de funcionar e devem ser reenviados como convites novos.
