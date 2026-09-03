# ADR-0002 - Progression Facts Are Separate from Progression Rules

## Status

Accepted for the TASK-008 progression boundary.

## Context

The internal `ProgressionInput` was extracted for engine execution and combines
activity facts with the configuration that determines their progression value.
Exposing that shape externally would allow a caller to choose XP, stress, and
distribution rules.

## Decision

Consumers provide `ProgressionFact` values and a
`ProgressionConfigurationReference`. Logos resolves
`ProgressionConfiguration` internally, combines it with the subject's current
skill state, and builds `ProgressionInput` for the existing core.

## Consequences

Rules remain centrally owned and future contracts can remain fact-oriented.
Logos must resolve configuration and eventually address configuration
versioning and lifecycle. Skill bonuses remain state-dependent effects rather
than static configuration values.

## Migration/compatibility impact

No HTTP endpoint, schema, authentication, LifeOS, or existing progression rule
changed. `AtividadeConfigId` is represented at the new boundary by an opaque
UUID reference and adapted through the existing repository.

## Validation

Factory composition, configuration resolution orchestration, stateful use case,
engine, profile, characterization, and persistence tests pass.
