# Migration Log

### 2026-09-03 — TASK-009R-V Validate External Subject Identity Migration

- Environment: PostgreSQL 16.10 reached through the existing local server using two isolated disposable databases: `logos_task009r_validation` (V31 fixture/backfill) and `logos_task009r_clean` (migration and regression suite). Docker/psql were unavailable, so no compose file was changed.
- Flyway: V1–V31 applied successfully before fixture creation; V32 then applied through Spring/Flyway with filesystem migration location. `flyway_schema_history` records version `32`, description `Create Progression Subject Identity Table`, `success=true`.
- Fixture/snapshot: 2 `app_user` and 2 `jogador` rows were inserted at V31. Their distinct progression state was level/xp/skill points `(3,1234,7)` and `(5,5678,11)`.
- Schema: `progression_subject_identity` has UUID `id`, `VARCHAR(64)` namespace, `VARCHAR(255)` external ID, UUID `jogador_id`, PK, FK, unique `(namespace, external_id)`, and both non-blank checks. The unique constraint provides the lookup index; no duplicate index exists.
- Backfill: 2 players, 2 `logos-native` mappings, missing `0`, orphan `0`, mismatched external IDs `0`, duplicate identities `0`. The recorded progression fields remained unchanged.
- Constraints: a second `experiment:subject-001` identity for one player was accepted; the same external identity for another player was rejected with PostgreSQL SQLSTATE `23505`. `experiment:ABC123` and `experiment:abc123` remained distinct.
- Resolver: `JpaExternalSubjectResolverPostgresIT` passed against the fixture database, resolving trimmed/mixed-case ` LOGOS-NATIVE ` and the backfilled AppUser UUID to the expected internal `SubjectId`.
- Tests: the complete directed identity/progression set passed with 37 tests. The full suite ran 158 tests: 156 passed and 2 pre-existing `RegistroVicioControllerTest` errors remained outside this task (`vicio_id`/vicio flow). No progression test failed.
- Result: real PostgreSQL/Flyway/backfill validation passed. TASK-009R is a reliable checkpoint for TASK-009H. No V32 change was required during validation.

### 2026-09-03 — TASK-009R Implement External Subject Identity Mapping

- Baseline: branch `feat/lsrv-16-implementar-rotina-de-registro-de-vicios`, HEAD `af7afd9`; reference checkpoint `af7afd9` and prior progression checkpoints preserved. Pre-existing working-tree changes were left untouched.
- Decision: use a dedicated `progression_subject_identity` table; a player may have many identities, while `(namespace, external_id)` is unique. `conexao_externa` and `ProvedorIntegracao` are not reused.
- Migration: `V32__Create_Progression_Subject_Identity_Table.sql` creates UUID PK, `VARCHAR(64)` namespace, `VARCHAR(255)` external ID, UUID FK to `jogador(id)`, simple non-blank checks, and backfills every existing `jogador` from `jogador.user_id` to `app_user.id` as `logos-native` plus the UUID string.
- Semantics: namespace is trim/lowercase using `Locale.ROOT`; external ID is trim/case-preserved. Lookup is explicit; no auto-create, email auto-link, LifeOS production mapping, update, or delete lifecycle was added.
- Resolver: `ExternalSubjectReference` -> `JpaExternalSubjectResolver` -> mapping -> `Jogador/AppUser` -> internal `SubjectId`. The resolver returns `NOT_FOUND` for an absent mapping. A thin application service delegates to `ExecuteConfiguredSubjectProgressionUseCase` without duplicating progression logic.
- Compatibility: `ProgressionEngine`, `ProgressionProfile`, `ExecuteProgressionUseCase`, `ProgressionInputFactory`, authentication, and the existing SubjectId HTTP endpoint were not changed.
- Tests: baseline directed progression tests remained green in the available 29-test gate from TASK-007R; new value-object, resolver, not-found, multiple-identity, delegation, and PostgreSQL resolver tests were added. Flyway/backfill validation is recorded in TASK-009R-V above.
- Remaining debts: external HTTP boundary, mapping lifecycle/onboarding, inter-service authentication, namespace authorization, configuration identity/versioning, idempotency, and concurrency.

### 2026-09-02 - TASK-007R Expose Internal Progression HTTP Adapter

- Baseline commit: `6e893e1`; blocker resolved by TASK-008; prior blocker record: `2359842`.
- Endpoint: `POST /api/internal/v1/progression/{subjectId}/evaluate`.
- Request: `configurationId` and fact `details` only; subject UUID is supplied in the path. Rules and current progression state are never accepted.
- Response: explicit DTO separates execution result (`globalXpDelta`, `stressTotal`, attribute deltas) from canonical updated profile.
- Mapping: HTTP DTO -> `SubjectId` + `ProgressionConfigurationReference` + `ProgressionFact` -> `ExecuteConfiguredSubjectProgressionUseCase` -> response DTO.
- Security: existing JWT protection remains active through `anyRequest().authenticated()`; no permit-all or authentication change.
- Errors: missing subject/configuration return 404 via dedicated progression exceptions and existing global advice; malformed UUID returns 400 via Spring MVC.
- Tests: 29 directed tests pass (5 controller tests plus 24 prior progression tests, including Spring processing integration).
- Limitations: UUID compatibility bridge, inter-service authentication, idempotency, concurrency, and configuration versioning remain future work.

### 2026-09-02 - TASK-007R Expose Internal Progression HTTP Adapter

- Baseline commit: `6e893e1`; blocker checkpoint: `2359842`.
- Endpoint: `POST /api/internal/v1/progression/{subjectId}/evaluate`.
- Request: `configurationId` plus numeric fact `details`; subject UUID is in the path. No rules or current progression state are accepted.
- Mapping: HTTP DTO -> `SubjectId` + `ProgressionConfigurationReference` + `ProgressionFact` -> `ExecuteConfiguredSubjectProgressionUseCase`.
- Response: explicit DTO separates calculation result (`globalXpDelta`, `stressTotal`, attribute deltas) from canonical updated profile.
- Security: existing JWT policy remains active; `/internal` was not added to permit-all routes.
- Errors: missing subject/configuration return 404 via dedicated progression exceptions and existing global advice; malformed path UUID returns 400 via Spring MVC.
- Tests: 29 tests executed in the final validation set (5 controller, 24 previous targeted); all green. The known vicio failures remain outside scope.
- Limitations: current subject/configuration UUIDs are compatibility bridges; inter-service authentication, idempotency, concurrency, and configuration versioning remain future work.

### 2026-09-02 - TASK-008 Establish Progression Fact and Configuration Resolution Boundary

- Baseline commit: `2359842`; architectural checkpoint: `1bf8522`.
- Configuration reference: `ProgressionConfigurationReference(UUID)` maps to the existing `AtividadeConfigId`; no migration was necessary.
- Facts: `ProgressionFact` contains only numeric detail keys and values.
- Configuration: `ProgressionConfiguration` contains base XP/stress, attribute distributions, XP rules, stress rules, and skill bonus rules.
- Resolution: `ProgressionConfigurationResolver` and `JpaProgressionConfigurationResolver` adapt `AtividadeConfigRepository` and current skill rule repositories without exposing JPA to the core.
- Composition: `ProgressionInputFactory` combines facts, resolved configuration, and `ProgressionProfile.skills` into the existing `ProgressionInput`; the configured stateful use case then delegates to the existing stateful/core services.
- Ownership: callers do not provide rules or progression state; Logos resolves rules and owns canonical state.
- Tests: 24 targeted tests pass with the existing resource-copy workaround.
- Risks: configuration versioning/lifecycle, temporary UUID bridge, idempotency, concurrency, stress association, and raw/effective XP remain debts.

### 2026-09-02 - TASK-007 Expose Internal Progression HTTP Adapter - BLOCKED

- Baseline commit: `1bf8522`; prior checkpoints: `4dac8a3`, `207b300`, `01b8caf`, `185916d`, `44df0a3`.
- Result: caminho B da tarefa; nenhum endpoint HTTP foi criado.
- Evidence: `ProgressionInput` combina fatos (`details`) com configuraÃ§Ã£o (`baseXp`, `baseStress`, `attributeDistributions`, regras XP/stress e bÃ´nus). O fluxo atual obtÃ©m toda essa configuraÃ§Ã£o de `RegistroAtividade.getAtividadeConfig()` e habilidades do jogador.
- Blocker: nÃ£o existe hoje uma porta para resolver configuraÃ§Ã£o por uma identidade de atividade/configuraÃ§Ã£o sem recebÃª-la integralmente do caller. Expor o input atual permitiria arbitrar regras de progressÃ£o.
- Options: (1) novo identificador de configuraÃ§Ã£o no request e resolver configuraÃ§Ã£o persistida no Logos; (2) contrato de fatos associado a uma atividade/configuraÃ§Ã£o existente, com adapter interno de montagem; (3) aceitar regras no request, rejeitado por transferir autoridade de regras ao caller.
- Recommendation: opÃ§Ã£o 2, aproveitando o fluxo atual de `AtividadeConfig` e mantendo regras no Logos; requer decisÃ£o/contrato antes do endpoint.
- Tests: baseline direcionado executado, 21 testes verdes; nenhuma alteraÃ§Ã£o de regra, API, schema, autenticaÃ§Ã£o ou vÃ­cios.

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
