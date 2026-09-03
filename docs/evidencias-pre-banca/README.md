# Evidências da análise de 02/09/2026

Estas evidências registram o estado anterior às correções recomendadas. As verificações isoladas reproduzem defeitos; não são testes de aceite aprovados da aplicação completa.

Revisões observadas, sem alterações prévias no código: Backend `5b188e3`, frontend `c9e0941`, Infra `ee28f9b`, e-mail `90b9da1`, PDF `8515444` e QR Code `79cc1a5`.

| Arquivo | Conteúdo |
|---|---|
| `browser-report.json` | Comportamentos observados no frontend compilado, com respostas HTTP simuladas |
| `browser-audit.cjs` | Script da verificação de navegador; inicia servidor estático temporário e o fecha ao terminar |
| `backend-isolado.txt` | Saída da verificação dos métodos de cadastro e participação |
| `CriticalFlowAudit.java` | Programa isolado com repositórios simulados e entidades fictícias |
| `eslint-report.json` | Resultado detalhado do ESLint, sem aplicar correções |
| `public-desktop.png`, `public-mobile.png` | Lista pública com dados fictícios em 1366px e 390px |
| `conclude-desktop.png` | Data incorreta na conclusão e disposição dos componentes |
| `certificates-desktop.png` | Exibição de 32/08 para um evento cuja data é 01/09/2026 |

As imagens são do frontend existente, usando fixtures para permitir inspeção sem banco. Fontes e recursos externos foram bloqueados no navegador; a aparência não valida seu carregamento pela internet. Não são capturas de uma demonstração integrada concluída.

## Builds

Foi executado `mvn -o -DskipTests clean package` em cada módulo Java, com `-Dmaven.repo.local` apontando ao cache existente desta máquina:

- Backend-Muttley: BUILD SUCCESS, 121 fontes de produção compiladas.
- Microservice-Pdf-Muttley: BUILD SUCCESS, 3 fontes de produção compiladas.
- Microservice-QrCode-Muttley: BUILD SUCCESS, 7 fontes de produção compiladas.
- Microservice-Email-Muttley: BUILD SUCCESS, 8 fontes de produção compiladas.

O backend apresentou avisos de MapStruct/Lombok; o QR Code apresentou aviso de API depreciada. Os testes foram explicitamente omitidos para não iniciar contextos conectados a serviços reais.

Frontend: dependências instaladas pelo lockfile com scripts de instalação desativados; `npm run build` passou. O lint apontou:

| Regra | Ocorrências |
|---|---:|
| `@typescript-eslint/no-explicit-any` | 33 |
| `react-hooks/set-state-in-effect` | 13 |
| `no-constant-binary-expression` | 3 |
| `@typescript-eslint/no-unused-vars` | 2 |
| `prettier/prettier` | 2 |
| `react-refresh/only-export-components` | 1 |
| `react-hooks/exhaustive-deps` | 1 aviso |

Total: 54 erros e 1 aviso. A compilação TypeScript passou apesar desses apontamentos do lint.

## Reprodução dos scripts

O script de navegador usa caminhos absolutos para o Playwright e o Chromium já instalados. Ajustar esses dois caminhos à máquina antes de executá-lo. Ele lê `front-muttley/dist`, que precisa ser criado com o build, e grava os resultados nesta pasta. Todas as respostas de API são simuladas; não acessar serviços reais para reproduzir esses cenários.

O programa Java precisa das classes compiladas do backend e de seu classpath de dependências. Nesta análise foi usado `mvn dependency:build-classpath`, seguido do modo de execução de arquivo-fonte do Java 21 com argumento de classpath. A execução precisou acessar dependências locais que não estavam disponíveis ao usuário restrito do sandbox. Não é necessário disponibilizar credenciais, banco ou rede para esse programa.
