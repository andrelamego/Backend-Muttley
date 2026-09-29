# Documentação do Muttley

Comece pelo [README do backend](../README.md) para executar o projeto. Este índice reúne as referências mantidas; planos antigos, prompts de implementação e evidências de pré-banca não fazem parte da documentação vigente.

| Diretório | Conteúdo |
| --- | --- |
| `regras/` | [Requisitos e regras](regras/requisitos-e-regras-de-negocio.md) e [cadastro por convite](regras/cadastro.md) |
| `api/` | [Contratos HTTP](api/README.md), [OpenAPI versionado](api/openapi.yaml) e [integrações](api/integracoes.md) |
| `operacao/` | [Execução e dados](operacao/README.md), [configuração](operacao/configuracao-backend.md) e atualização da massa local |
| `testes/` | [Execução e limites das suítes](testes/README.md) e [matriz RF/RN → testes](testes/matriz-requisitos-testes.csv) |
| `diagramas/` | [Casos de uso, classes e entidades](diagramas/README.md) |

## Como manter

- Atualize a referência do assunto ao mudar um contrato ou uma regra; evite criar outro relatório datado para repetir o mesmo conteúdo.
- Os controllers, DTOs e entidades são a fonte técnica dos contratos. Mantenha o OpenAPI estático sincronizado com eles.
- Resultados de testes e capturas de uma execução são artefatos regeneráveis. Guarde-os em diretórios de saída ignorados pelo Git, sem tratá-los como garantia permanente de cobertura.
- Preserve os IDs RF/RN ao atualizar requisitos e acompanhe suas mudanças na matriz de rastreabilidade.
- Decisões novas de arquitetura podem ter ADR próprio; instruções temporárias para um agente não substituem a documentação do produto.
