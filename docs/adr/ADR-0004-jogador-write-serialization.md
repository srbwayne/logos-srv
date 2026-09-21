# ADR-0004: Jogador write serialization

## Context

The supported progression path already serialized updates with a PostgreSQL
`PESSIMISTIC_WRITE` lock on the `jogador` row. An unlocked profile update could
load a stale aggregate, allow progression to commit, and then overwrite the
new XP with the stale scalar state. This was reproduced deterministically.

## Decision

Supported transactional writers that mutate an existing `Jogador` or related
mutable state acquire the same `Jogador` row lock before reading or changing
aggregate state. Profile and nickname updates, RegistroVicio creation, and
RegistroVicio processing therefore participate in the protocol. Read-only
queries remain unlocked.

Child state mutated by these workflows participates in serialization through
the parent row. This slice proves the stress path and the VicioJogador load
ordering. `DebuffJogador` has no `(jogador_id, debuff_id)` uniqueness
constraint, so duplicate-child defense remains follow-up work; attributes and
skills retain their existing constraints and progression-owned write paths.

No `@Version` or `@DynamicUpdate` is introduced in this slice; the protocol is
pessimistic and consistent with progression.

## Consequences and limits

The lock prevents stale aggregate writes among participating supported writers.
Direct repository writes and unsupported writers are not magically protected.
Database constraints may still be desirable as defense in depth for child
uniqueness. Lock timeouts and deadlock retries remain unconfigured.
