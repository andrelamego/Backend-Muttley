# Atualização da massa local de eventos

O script reprograma eventos vencidos para datas relativas ao momento da execução, no fuso `America/Sao_Paulo`. A conexão é lida diretamente de `src/main/resources/application.properties`; o script não executa o carregamento de configuração do Spring.

## Execução

Requisitos: Java 21 e o MariaDB Connector/J no cache Maven local. Na pasta do backend, execute no PowerShell:

```powershell
# Somente mostrar a proposta, sem alterar registros.
./scripts/atualizar-massa-eventos.ps1

# Salvar a cópia das agendas anteriores e aplicar em uma transação.
./scripts/atualizar-massa-eventos.ps1 -Aplicar
```

Se o Java não estiver no PATH, informe `-JavaCommand 'C:/Program Files/Java/jdk-21/bin/java.exe'`. Para um driver fora do cache Maven, use `-DriverPath` com o caminho do JAR.

## Critérios

- Seleciona apenas eventos com horário de término passado, status `CRIADO` ou `EM_ANDAMENTO`, sem presenças confirmadas e sem certificados.
- Com pelo menos três eventos elegíveis, prepara um em andamento no momento da execução, outro para algumas horas depois e os demais para os próximos dias e semanas. Perto da virada do dia, ajusta os horários para manter início e fim na mesma data.
- Com menos de três eventos elegíveis, programa todos para datas futuras.
- Mantém IDs, inscrições, capacidades, usuários e certificados. Eventos cancelados ou finalizados são preservados.
- A atualização é manual e exclusiva da base local. Não inicia a aplicação nem publica notificações.
- Uma nova execução imediata não altera novamente os eventos reprogramados. Em execuções posteriores, somente eventos que voltaram a vencer e continuam elegíveis entram no planejamento.

## Cópia e reversão

Antes das alterações, grava `agendas.csv` e `restaurar.sql` em `target/backups-massa/<data-hora>-<identificador>/`. O arquivo `aplicado.txt` registra a confirmação da transação quando sua gravação é possível.

Confira o CSV e o estado do banco antes de executar o SQL de reversão em um cliente MySQL. Ele restaura somente agendas ainda iguais às aplicadas e sem presenças ou certificados. Copie essa pasta para outro local se precisar guardar o histórico: `mvn clean` remove `target`.


## Limitação de configuração

O script Java ainda não resolve a importação do arquivo privado da raiz nem variáveis de ambiente. Depois da separação da senha local, seu carregamento de configuração precisa ser adaptado antes de usar este procedimento com o ambiente descrito em [configuração do backend](configuracao-backend.md). Não copie credenciais para o arquivo versionado para contornar essa limitação.
