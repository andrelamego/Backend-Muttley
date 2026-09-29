# Configuração do backend

`src/main/resources/application.properties` contém os valores compartilhados e as opções que diferem dos padrões das bibliotecas. Os endereços locais usam MariaDB na porta `3307`, Kafka na `9092`, PDF na `8084`, QR Code na `8086` e frontend na `5173`.

## Executar a API fora do Docker

Na raiz de `Backend-Muttley`, crie o arquivo de configuração privada:

```powershell
if (-not (Test-Path application-local.properties)) {
    Copy-Item application-local.properties.example application-local.properties
}
```

Edite `spring.datasource.password` nesse arquivo com a senha do banco. Para usar o MariaDB do Compose, ela deve corresponder a `MUTTLEY_DB_PASSWORD` em `Infra-Muttley/.env`. Depois execute `mvn spring-boot:run`.

O arquivo local é importado automaticamente quando a API é executada a partir da raiz do backend e está ignorado pelo Git. Se precisar alterar outros valores apenas nessa máquina, adicione suas propriedades nele; por exemplo, `app.frontend.url=http://localhost:5174`. Evite guardar credenciais em `src/main/resources/application.properties`.

## Executar pelo Compose

O Compose fornece conexão do banco, senha, Kafka, URLs dos microsserviços e demais configurações pelas variáveis de ambiente já existentes. Elas têm prioridade sobre os arquivos de propriedades. O Dockerfile copia apenas `pom.xml` e `src`, portanto o arquivo privado da raiz não entra na imagem.

## Padrões mantidos

- O Kafka usa `StringSerializer` para chaves e `StringDeserializer` para chaves e valores recebidos, definidos pelo Spring Boot. A publicação de valores continua usando `JsonSerializer` explicitamente.
- O grupo do consumidor continua definido no `@KafkaListener` de `QrCodeResponseConsumer`; a opção de pacotes confiáveis foi removida porque o consumidor recebe strings e faz a conversão para JSON no código.
- O JWT mantém a expiração de duas horas, definida como padrão no `JwtService`.
- O Swagger mantém `/v3/api-docs` e `/swagger-ui.html`, caminhos padrão do Springdoc.
- O resolver de dialeto permanece para compatibilidade com o MariaDB 10.4 usado nos testes e ambientes anteriores.
- A aplicação usa endpoints REST e não precisa do filtro de métodos ocultos de formulários HTML.

Os testes de integração continuam usando `src/test/resources/application-test.properties`, que define banco, URLs e chave JWT próprios.
