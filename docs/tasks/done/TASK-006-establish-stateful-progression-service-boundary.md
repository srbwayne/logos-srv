# TASK-006 - Establish Stateful Progression Service Boundary

Status: done

Baseline: `4dac8a3`.

## Outcome

Introduced `SubjectId`, `ProgressionProfileRepository`,
`ExecuteSubjectProgressionUseCase`, and
`StatefulProgressionApplicationService`. The stateful service loads the
canonical profile, composes the existing stateless progression use case,
persists the updated profile, and returns `ProgressionOutcome`.

The persistence adapter temporarily maps `SubjectId(UUID)` to the existing
`AppUserId`/`Jogador.user_id` relationship. No new schema was required.

## Validation

Twenty-one targeted tests pass, including subject, stateful orchestration,
application, engine, profile, characterization, service, and persistence tests.
No REST, authentication, LifeOS, migration, event, transaction, or vicio
behavior was changed.
