# Muttley — roteiro de preparação e apresentação

Planejamento proposto em 02/09/2026. Ajustar datas, duração e entregáveis ao regulamento e às orientações do professor. Este documento não substitui a rubrica da instituição.

## 1. Mensagem central

“O Muttley integra a organização de eventos acadêmicos, as inscrições, o registro de presença e a emissão de certificados consultáveis por código.”

Começar pelo problema de quem organiza e participa dos eventos. Mostrar as tecnologias quando elas explicarem uma decisão. Medalhas são um complemento; sua existência não comprova aumento de engajamento.

O que é demonstrável pelo código: funcionalidades implementadas, arquitetura, BCrypt/JWT, emissão por presença e geração de PDF. O que ainda exige evidência: redução de trabalho, facilidade de uso, confiabilidade ponta a ponta, desempenho e aceitação pelos usuários.

## 2. Escopo mínimo da demonstração

| Etapa | Ação | Evidência esperada |
|---|---|---|
| Organizador | Criar evento com cadastros auxiliares preparados | Evento aparece publicamente e possui QR de inscrição |
| Participante | Inscrever-se com dados fictícios | Número de inscrição e pessoa vinculada ao evento |
| Presença | Confirmar durante a janela definida | Uma presença registrada; repetição não duplica efeitos |
| Organizador | Conferir presentes e concluir com assinatura | Evento finalizado; certificados somente para elegíveis |
| Participante | Abrir a área pessoal e baixar certificado | Nome, tema, data, carga horária, assinatura visual e código corretos |
| Verificador | Abrir o código em aba anônima | Documento consultável sem login; código inválido tratado corretamente |

O fluxo é uma meta para o ensaio após as correções do [relatório](analise-pre-banca-2026-09-02.md), não uma afirmação de que a integração inteira já passou.

## 3. Dados e ambiente

Preparar um conjunto fictício que possa ser restaurado sem afetar o banco de desenvolvimento. Evitar nomes e contatos reais na projeção e em capturas. E-mails fictícios devem ser capturados por um mecanismo de demonstração, sem entrega externa.

Usar pelo menos dois perfis de acesso, um local, endereço, disciplina e patrocinador. Ter um evento futuro para inscrição, um em andamento com participantes preparados, um finalizado com certificado e um cancelado para validar bloqueios. Preparar participantes presentes, ausentes e já certificados.

Se quiser demonstrar toda a jornada no mesmo evento, agendar o início poucos minutos após a inscrição e ensaiar esse tempo. Como alternativa, explicar que os eventos preparados representam etapas diferentes. Não alterar registros manualmente durante a fala para fingir que uma transição ocorreu.

| Dependência atual | Preparação |
|---|---|
| MySQL | Banco de demonstração separado, carga inicial e restauração testada |
| Kafka/ZooKeeper | Subir antes das aplicações; verificar consumidores e tópicos |
| Backend 8083 | Variáveis de ambiente, banco correto e administrador de demonstração |
| PDF 8084 | Chromium instalado, recursos acessíveis e assinatura disponível |
| E-mail 8085 | Configuração OAuth preparada; capturar envio em ambiente de teste |
| QR 8086 | QuickChart acessível ou alternativa local previamente implementada |
| Frontend 5173 em desenvolvimento | API configurada; acesso pelo celular se fizer parte da apresentação |
| Arquivos de assinatura | Persistência e caminho consistentes; imagem de exemplo previamente conferida |

Um build frontend estático não herda automaticamente o proxy do servidor de desenvolvimento. Documentar como `/api`, rotas da SPA e URLs públicas funcionarão na forma de execução escolhida. Preferir um endereço canônico para compartilhar certificados. Conferir também o link montado para o LinkedIn, que hoje deriva da origem da requisição ao backend.

## 4. Testes que devem sustentar a apresentação

Os resultados devem ser registrados como aprovado, reprovado ou não executado, com data e evidência. Os testes abaixo são uma seleção para implementar/ensaiar; o guia de testes existente detalha a expansão.

| ID | Cenário | Aceite |
|---|---|---|
| PB-01 | Cadastro público recebe ID de pessoa existente | Pessoa existente permanece inalterada |
| PB-02 | Completar cadastro sem token, com token vencido e reutilizado | Rejeitar; permitir somente token válido de uso único |
| PB-03 | ADMIN se inscreve em evento | Continua ADMIN no banco e após novo login |
| PB-04 | USER consulta/altera participação alheia | Rejeitar; próprias consultas usam escopo do usuário |
| PB-05 | Visitante/USER chama rota administrativa | 401/403 conforme autenticação |
| PB-06 | Mesma pessoa se inscreve duas vezes | Uma inscrição; conflito compreensível na segunda |
| PB-07 | CPF inválido ou formatos diferentes do mesmo CPF | Rejeitar inválido; normalizar identidade sem duplicar pessoa |
| PB-08 | Presença antes/depois da janela, em evento cancelado/finalizado | Respeitar a política aprovada |
| PB-09 | Repetir confirmação de presença | Não duplicar medalha nem registro |
| PB-10 | Desmarcar presença já confirmada na conclusão | Interface e persistência seguem a mesma regra |
| PB-11 | Concluir com presentes e ausentes | Somente os elegíveis recebem certificado |
| PB-12 | Repetir a conclusão/geração | Não gerar duplicados |
| PB-13 | Abrir certificado em aba anônima | Sem login, dados mínimos e corretos |
| PB-14 | Código inexistente | Mensagem de certificado não encontrado |
| PB-15 | Datas no primeiro/último dia do mês | Dia/mês consistentes em listas, conclusão e PDF |
| PB-16 | Remover participante e recarregar edição | Remoção persistida ou impedimento explicado |
| PB-17 | PDF com nome/tema longo, acentos e assinatura | Sem cortes; código/URL de validação legíveis |
| PB-18 | Kafka, PDF ou e-mail indisponível | Estado consistente; falha visível e recuperação definida |
| PB-19 | Recriar ambiente conforme README | Outro procedimento de execução não é necessário |
| PB-20 | Smartphone e projetor | Links abrem no dispositivo correto; conteúdo legível |

Priorizar testes unitários/HTTP para as regras de segurança e negócio, testes com banco para unicidade/transação e poucos testes de navegador para a jornada central. Os ensaios isolados desta revisão não substituem PB-01 a PB-20 com a versão corrigida e a infraestrutura real.

## 5. Estrutura sugerida de apresentação

Exemplo para 15 minutos, a adaptar ao tempo concedido. Cerca de 10 slides é suficiente.

| Tempo | Conteúdo | O que levar |
|---|---|---|
| 0:00–1:00 | Contexto e problema | Exemplo real do processo atual, sem dados pessoais |
| 1:00–2:30 | Objetivos, público e escopo | Objetivo geral, objetivos específicos e limite da entrega |
| 2:30–4:00 | Levantamento e alternativas | Como requisitos foram obtidos e critérios de comparação |
| 4:00–6:00 | Solução e arquitetura | Diagrama legível; razão para API, banco, PDF e mensageria |
| 6:00–7:00 | Modelo e regras centrais | Pessoa, evento, participação, certificado e elegibilidade |
| 7:00–11:00 | Demonstração | A jornada descrita acima, com etapas preparadas |
| 11:00–13:00 | Validação e resultados parciais | Testes executados e observações reais, com limites |
| 13:00–14:00 | Limitações | Pendências relevantes e decisões ainda abertas |
| 14:00–15:00 | Próximas entregas | Cronograma e contribuição esperada |

Não mostrar todos os CRUDs. O avaliador precisa entender a integração e a regra de emissão. Capturas de tela podem apoiar a explicação de etapas secundárias e preservar tempo para a demonstração central.

## 6. Perguntas para ensaiar

| Pergunta provável | Como preparar a resposta |
|---|---|
| Qual problema real o Muttley resolve? | Relatar o processo observado e as dificuldades confirmadas com os envolvidos |
| Por que não usar uma solução já existente? | Comparar necessidades institucionais por critérios iguais; reconhecer funcionalidades compartilhadas |
| Por que microsserviços e Kafka? | Explicar isolamento do PDF e processamento de notificações; reconhecer o custo operacional e a alternativa de um monólito modular |
| Como o sistema garante que alguém participou? | Descrever a política de presença efetivamente implementada; reconhecer os limites de CPF e QR compartilhável |
| O certificado possui assinatura digital? | Hoje há assinatura visual/textual e código de consulta; não há assinatura criptográfica implementada |
| O que o UUID garante? | Identifica o registro de emissão; sozinho não detecta toda alteração em um PDF externo |
| O que acontece se o e-mail falhar? | Mostrar a estratégia implementada de registro/retentativa; no código atual há log, e o certificado é enviado por link |
| Como impedir certificados duplicados? | Explicar regra, restrição no banco e testes concorrentes quando implementados |
| Como os dados pessoais são protegidos? | Mostrar escopo das rotas, DTO mínimo, senhas protegidas e decisões de retenção/acesso |
| Como você avaliou qualidade e utilidade? | Separar builds, testes funcionais, teste integrado e avaliação por usuários |
| O que falta para a banca final? | Apresentar entregas priorizadas com critérios de conclusão |

Evitar afirmações como “totalmente seguro”, “antifraude”, “altamente escalável”, “aumenta o engajamento” ou “reduz em X% o trabalho” sem medidas e controles que as sustentem.

## 7. Sequência de preparação

Se houver aproximadamente duas semanas disponíveis, usar a distribuição abaixo como referência, não como estimativa fechada de esforço.

| Bloco | Entrega |
|---|---|
| Dias 1–3 | Cadastro, ativação, autorização, presença e testes de regressão |
| Dias 4–5 | Certificado público, datas, seleção de presença e contratos da demonstração |
| Dias 6–7 | Ambiente, dados de demonstração e validação real de PDF/QR/notificações |
| Dias 8–9 | Texto, diagramas, matriz de requisitos/testes e resultados parciais |
| Dias 10–11 | Slides, roteiro falado e duas repetições da jornada |
| Dias restantes | Correções encontradas no ensaio e margem para imprevistos |

Se o prazo for muito curto, concentrar esforço em P0, na jornada central, no material acadêmico exigido e em uma demonstração honesta do estágio atual. Reservar mudanças amplas de arquitetura, CSS e funcionalidades complementares para depois.

## 8. Checklist do ensaio final

- [ ] Confirmar com orientador data, tempo de fala, documentos e critérios de avaliação.
- [ ] Atualizar os três diagramas existentes para refletir entidades, atributos, atores e permissões atuais.
- [ ] Congelar uma versão reproduzível e registrar os commits dos seis repositórios.
- [ ] Validar build e lint; executar os testes selecionados e guardar os resultados.
- [ ] Restaurar dados de demonstração e iniciar tudo pelo procedimento documentado.
- [ ] Conferir relógio, fuso, datas e estados dos eventos preparados.
- [ ] Confirmar nomes, carga horária, assinatura e verificação do PDF baixado.
- [ ] Abrir consulta pública em janela anônima e no celular, se usado.
- [ ] Conferir resolução/zoom do projetor e legibilidade das tabelas.
- [ ] Gravar um vídeo curto da versão realmente funcionando, identificado como contingência.
- [ ] Salvar slides em PDF, capturas e um certificado de exemplo localmente.
- [ ] Ensaiar duas vezes com cronômetro e preparar respostas às limitações.

O vídeo de contingência preserva a apresentação em caso de falha de rede ou equipamento; ele não substitui explicar pendências reais da versão apresentada.
