# Release do backend

## Identificação

| Item | 0.1.0-alpha |
| --- | --- |
| Branch de preparação | `release/0.1.0-alpha` |
| Versão Maven e OpenAPI | `0.1.0-alpha` |
| Tag de publicação | `v0.1.0-alpha` |
| Artefato executável | `target/Muttley-0.1.0-alpha.jar` |
| Notas da versão | [Changelog](../../CHANGELOG.md) |

A preparação do branch não publica a release. A tag deve apontar para o commit final aprovado, incorporado ao branch de distribuição do repositório. Mantenha `pom.xml`, `OpenApiConfig`, seu teste e `docs/api/openapi.yaml` com a mesma versão em cada entrega.

## Serviços necessários

A API usa Java 21, MariaDB/MySQL e Kafka. PDF, QR Code e email são entregas separadas. O [Infra-Muttley](https://github.com/andrelamego/Infra-Muttley) contém o Compose e as instruções de configuração; os repositórios precisam estar em pastas irmãs para o build local.

Estas são as revisões disponíveis no ambiente local de preparação desta alpha. Ao validar outra combinação, registre as revisões utilizadas nas notas da publicação:

| Repositório | Revisão |
| --- | --- |
| [Infra-Muttley](https://github.com/andrelamego/Infra-Muttley) | `69a30f9` |
| [Microservice-Pdf-Muttley](https://github.com/andrelamego/Microservice-Pdf-Muttley) | `9372496` |
| [Microservice-QrCode-Muttley](https://github.com/andrelamego/Microservice-QrCode-Muttley) | `132b49d` |
| [Microservice-Email-Muttley](https://github.com/andrelamego/Microservice-Email-Muttley) | `c9adf8e` |

Siga a [configuração do backend](configuracao-backend.md) para credenciais, URL do frontend, JWT e serviços. Configure um segredo JWT próprio fora do ambiente local. A primeira base exige schema vazio; bases que já têm `V1` aplicada são preservadas. Não recrie volumes ao atualizar uma base já gerenciada pelo Flyway.

## Validar e empacotar

Com Java 21, Maven e Docker ativos, execute na raiz do backend:

```powershell
# Testes unitários, integração MySQL, Flyway e empacotamento.
mvn clean -Pintegration verify

# Compatibilidade com MariaDB no banco descartável da suíte.
mvn -Pintegration '-Dmuttley.test.database=mariadb' verify

# Hash do artefato final, para distribuir junto ao JAR.
$jarRelease = 'target/Muttley-0.1.0-alpha.jar'
$hashRelease = (Get-FileHash -LiteralPath $jarRelease -Algorithm SHA256).Hash.ToLowerInvariant()
"$hashRelease  Muttley-0.1.0-alpha.jar" | Set-Content -Encoding ascii "$jarRelease.sha256"
```

Confira `BUILD SUCCESS` e ausência de falhas/erros. Relatórios ficam em `target/surefire-reports` e `target/failsafe-reports`; não os versione. O JAR e seu SHA-256 também são artefatos de saída, e não arquivos do código-fonte.

Para executar o JAR com os serviços configurados, use `java -Duser.timezone=America/Sao_Paulo -jar target/Muttley-0.1.0-alpha.jar`. A configuração privada deve estar no diretório de execução ou nas variáveis de ambiente.

## Conferir a integração real

Na pasta da infraestrutura, reconstrua a API e aguarde os serviços ficarem saudáveis:

```powershell
docker compose up --build -d --wait
docker compose ps
docker compose logs api
```

Para testes manuais com dados fictícios, use o arquivo adicional e as contas descritas na [massa de testes](massa-de-testes.md). A massa é opcional, fica fora das migrações e exige base vazia para a primeira carga.

Confira a disponibilidade em `GET /api/inicio`, a versão `0.1.0-alpha` em `/v3/api-docs` e o Swagger em `/swagger-ui.html`. Depois percorra login ADMIN/USER, dashboard, inscrição, presença, conclusão com PNG/JPG, consulta pública e download de um PDF. Confirme os QR Codes e a chegada das notificações no Mailpit. A suíte do backend, isoladamente, não comprova essa cadeia real.

## Publicar

Após validação e revisão do branch, incorpore seus commits ao branch de distribuição e confira que o commit escolhido contém a versão final. Crie a tag anotada `v0.1.0-alpha` nesse commit e envie-a ao remoto. No GitHub, publique uma **pré-release**, com o título `Muttley Backend 0.1.0-alpha`, as notas do changelog, o JAR e seu SHA-256.

Registre o commit do backend e as revisões dos serviços efetivamente validadas. Cada repositório mantém suas próprias tags; a tag do backend não versiona automaticamente a infraestrutura ou os microsserviços. Mudanças de schema posteriores usam novas migrações, sem editar `V1`.
