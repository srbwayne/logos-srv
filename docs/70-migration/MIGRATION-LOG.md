# Migration Log

Append one entry per completed slice.

## Template

### YYYY-MM-DD — <slice>

- Baseline commit:
- Objective:
- Behavior preserved/changed:
- Files changed:
- Tests executed:
- Result:
- Decisions:
- Remaining risks:
- Next recommended slice:

### 2026-09-02 — TASK-002 Extract Progression Calculation Model

- Baseline commit: `44df0a3` (`test: characterize activity progression processing`)
- Objective: separar os cálculos de progressão da orquestração de `ProcessarRegistroAtividadeService` sem alterar regras.
- Behavior preserved/changed: comportamento preservado; nenhuma migration, API, autenticação ou regra de XP/estresse foi alterada.
- Files changed: `ProcessarRegistroAtividadeService`, `ProgressionEngine`, `ProgressionResult`, `XpCalculator`, `StressCalculator`, `SkillBonusCalculator` e testes de caracterização.
- Tests executed: 9 testes unitários de caracterização; 2 testes de integração de processamento/persistência; suíte completa não considerada gate por falhas conhecidas em `RegistroVicioControllerTest`.
- Result: testes direcionados verdes; o serviço agora calcula via `ProgressionEngine` e aplica/persiste o resultado.
- Decisions: manter `NivelXPService` como responsável pela progressão de nível; preservar truncamento, pesos e semântica de cortes existentes.
- Remaining risks: regras de estresse não possuem associação explícita a fator no modelo e são avaliadas por detalhe; XP bruto persistido difere do XP líquido usado para transição; suíte completa continua limitada pelos erros de vícios e pela codificação de `application.properties` já modificados.
- Next recommended slice: revisar, com decisão explícita, o contrato interno do motor e a idempotência do processamento antes de qualquer integração externa.
