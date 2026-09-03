# TASK-004 - Establish Progression Profile Boundary

Status: done

Baseline: `01b8caf` (behavioral checkpoint `44df0a3`; prior architectural checkpoint `185916d`).

## Outcome

Introduced the JPA-independent `ProgressionProfile` state boundary and a
`ProgressionProfileMapper` at the application edge. Profile application uses
the pure `ProgressionLevelCalculator`, preserving raw XP persistence,
stress-floor behavior, level thresholds, and skill-point grants.

`ProgressionEngine` remains independent of `Jogador` and persistence. The
orchestrator maps the current aggregate to a profile, applies the calculation
result, maps the state back, and persists the existing entities.

## Validation

Direct profile tests, engine tests, characterization tests, service tests, and
processing/persistence tests pass using the existing `maven.resources.skip=true`
workaround for the pre-existing `application.properties` encoding issue.

## Limitations

No migrations, REST/authentication changes, LifeOS integration, or vicio-area
changes were made. Existing debts around ID-derived keys, stress-factor
association, raw/effective XP, idempotency, and the broad `Jogador` aggregate
remain for later slices.
