# Migration Strategy

## DECIDED: evolutionary migration, not rewrite

The existing repository remains the migration base.

### Sequence

1. Freeze current behavior with characterization tests.
2. Record baseline and architecture violations.
3. Extract pure calculation policies from `ProcessarRegistroAtividadeService` without changing results.
4. Introduce a cohesive `ProgressionEngine` facade over proven policies.
5. Decouple progression identity from personal/authentication data.
6. Define an internal evaluation contract.
7. Integrate one LifeOS fact in shadow mode.
8. Compare current LifeOS result versus Logos result.
9. Cut over authority only after explicit acceptance.
10. Remove obsolete auth/profile code only after no consumer depends on it.

## Strangler rule

Old behavior remains callable until its replacement is tested and integrated. Each slice should leave the repository releasable.

## Physical modularization

Extraction into modules such as `logos-domain`, `logos-engine` and `logos-api` is deferred until dependency boundaries are stable and enforceable.
