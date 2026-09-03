# TASK-005 - Establish Progression Application Contract

Status: done

Baseline: `207b300`.

## Outcome

Created the internal `ExecuteProgressionUseCase` port and its
`ProgressionApplicationService` implementation. The service receives only
`ProgressionInput` and the current `ProgressionProfile`, invokes
`ProgressionEngine`, applies the result, and returns `ProgressionOutcome`.

`ProcessarRegistroAtividadeService` remains the RegistroAtividade adapter:
it loads and maps the source, invokes the use case, applies the updated profile
to `Jogador`, marks the record processed, and persists both entities.

## Validation

Seventeen targeted tests pass, including direct application-contract tests,
engine/profile characterization, service characterization, and persistence
integration tests. No REST, migration, authentication, event, or vicio changes
were made.
