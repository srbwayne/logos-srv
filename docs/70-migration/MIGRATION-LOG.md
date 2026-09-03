# Migration Log

### 2026-09-02 - TASK-006 Establish Stateful Progression Service Boundary

- Baseline commit: `4dac8a3`; prior checkpoints: `207b300`, `01b8caf`, `185916d`, `44df0a3`.
- Decision: Logos owns canonical progression state; consumers provide subject identity and progression facts, not the full current state.
- Subject: `SubjectId(UUID)` is an opaque value object independent of LifeOS/JWT; it temporarily maps to the existing `AppUserId` UUID through `Jogador.user_id`.
- Repository boundary: `ProgressionProfileRepository` exposes only `SubjectId` and `ProgressionProfile`; `JpaProgressionProfileRepository` adapts current repositories and mapper.
- Stateful use case: `ExecuteSubjectProgressionUseCase` and `StatefulProgressionApplicationService` load, delegate, save, and return `ProgressionOutcome`.
- Missing state: `NOT_FOUND` semantics via `IllegalStateException`; current registration provides no evidence for automatic profile creation.
- Persistence: no migration was necessary; existing transaction behavior was preserved.
- Tests: 21 targeted tests pass with the existing resource-copy workaround.
- Risks: temporary UUID mapping is not general external identity mapping; lost updates, idempotency, raw/effective XP, stress association, and profile lifecycle remain debts.

### 2026-09-02 - TASK-005 Establish Progression Application Contract

- Baseline commit: `207b300`; architectural checkpoints: `01b8caf`, `185916d`; behavioral checkpoint: `44df0a3`.
- Contract: `ExecuteProgressionUseCase.execute(ProgressionInput, ProgressionProfile)` returns `ProgressionOutcome`.
- Application service: `ProgressionApplicationService` coordinates engine calculation and profile application without source, JPA, repository, HTTP, auth, or LifeOS dependencies.
- Outcome: preserves both `ProgressionResult` (needed by the current record adapter) and updated `ProgressionProfile`; no command was necessary.
- Responsibilities removed: semantic progression execution and result application are delegated by `ProcessarRegistroAtividadeService`; record lookup, mapping, status, and persistence remain there.
- Behavior preserved: XP, stress, attributes, skill bonuses, levels, points, status, and persistence remain unchanged.
- Tests: 17 targeted tests pass using the existing resource-copy workaround for the pre-existing `application.properties` encoding issue.
- Remaining risks: raw/effective XP, stress association, ID-derived keys, idempotency, persistence boundary, and existing non-progression responsibilities in `Jogador`.

### 2026-09-02 - TASK-004 Establish Progression Profile Boundary

- Baseline commit: `01b8caf`; checkpoints: `185916d` and `44df0a3`.
- Objective: explicit progression state boundary independent of the persistent `Jogador` aggregate.
- Progression state: global XP/level, stress, skill points, attribute progressions, and skill state.
- Components: immutable `ProgressionProfile`, pure `ProgressionLevelCalculator`, and `ProgressionProfileMapper` at the application boundary.
- Behavior preserved: formulas, truncation, stress floor, level transitions, points, and persistence remain unchanged.
- Dependencies eliminated from profile/engine: JPA, Spring, repositories, HTTP, authentication, and LifeOS.
- Dependencies remaining: mapper/orchestrator know current `Jogador`/JPA; `NivelXPService` remains external to the engine.
- Tests: profile, engine, characterization, service, and persistence-focused tests pass with the known resource-copy workaround.
- Risks: ID-derived keys, stress-factor association debt, raw/effective XP distinction, idempotency, and non-progression responsibilities in `Jogador`.

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

### 2026-09-02 — TASK-003 Decouple Progression Engine Input

- Baseline commit: `185916d` (`refactor: extract progression calculation model`); behavioral checkpoint: `44df0a3`.
- Objective: estabelecer uma entrada própria para o cálculo e remover `RegistroAtividade` do núcleo do engine.
- Input contract: `ProgressionInput` imutável com bases, detalhes numéricos, distribuições/regras e bônus de habilidade, identificados por chaves escalares.
- Mapping: `ProcessarRegistroAtividadeService.toProgressionInput`; somente o mapper conhece `RegistroAtividade` e entidades JPA.
- Dependencies eliminated: `ProgressionEngine` não depende mais de RegistroAtividade, entidades JPA, repositories ou Spring.
- Dependencies remaining: o mapper e o serviço de aplicação continuam acoplados às entidades atuais; `NivelXPService` permanece externo ao engine.
- Behavior preserved/changed: fórmulas, cortes, pesos, truncamento, acumulação, bônus, nível e persistência preservados.
- Tests executed: 2 testes diretos do engine, 3 de `NivelXPService`, 7 de caracterização e 2 de integração/persistência; 14 testes direcionados verdes.
- Remaining risks: chaves são derivadas dos IDs atuais; regras de estresse seguem sem fator explícito; mutabilidade das entidades e idempotência permanecem fora desta fatia.
- Next recommended slice: avaliar se o resultado deve ser aplicado por um componente separado, sem antecipar integração LifeOS.
