# ADR-0001 - Logos Owns Canonical Progression State

## Status

Accepted for the TASK-006 progression boundary.

## Context

The progression engine is deterministic and can run statelessly, but external
consumers must not maintain competing copies of XP, levels, stress, attributes,
skills, or skill points. The current Logos persistence already stores this
state inside `Jogador`, associated with `AppUser`.

## Decision

Logos owns the canonical progression state for each `SubjectId`. Stateful
application orchestration loads the profile, delegates calculation to the
existing stateless use case, persists the updated profile, and returns the
outcome. `SubjectId` temporarily wraps the existing `AppUserId` UUID because no
independent external-subject schema exists yet.

## Alternatives considered

- Keep state in consumers: rejected because it duplicates progression authority.
- Add a new subject table now: deferred because it requires schema migration and
  no current behavior requires it.
- Make the engine persistent: rejected because calculation remains separate from
  state orchestration.

## Consequences

Positive: one canonical authority, simpler consumers, and coherent rules/state.

Negative: Logos owns load/save lifecycle; identity mapping, concurrency,
idempotency, and persistence consistency become service concerns.

## Migration/compatibility impact

No schema, REST, authentication, event, or LifeOS changes. The existing
`RegistroAtividade` adapter remains on the stateless path. The new stateful
boundary uses `JogadorRepository.findByAppUserId` internally.

## Validation

Subject value tests, stateful orchestration tests, application/engine/profile
tests, characterization tests, and persistence integration tests pass.
