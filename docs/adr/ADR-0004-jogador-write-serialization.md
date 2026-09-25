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
the parent row. PostgreSQL characterization covers the real supported
production paths:

* Two concurrent `CreateRegistroVicioService` calls for the same Jogador and
  Vicio preserved one logical `VicioJogador` and created two `RegistroVicio`
  rows, without a duplicate child, deadlock, or timeout.
* Two concurrent `ProcessarRegistroVicioService` calls for the same Jogador
  and Debuff preserved one logical `DebuffJogador`; the configured penalty of
  7 accumulated serially to the expected final potency of 14, without lost
  accumulation, a duplicate child, deadlock, or timeout.

The parent lock is therefore sufficient for the currently supported production
paths. `VicioJogador` has no database `UNIQUE (jogador_id, vicio_id)` and
`DebuffJogador` has no database `UNIQUE (jogador_id, debuff_id)`. Adding those
constraints remains a defense-in-depth opportunity, not a confirmed production
defect or a correction required by the characterized supported paths.

No `@Version` or `@DynamicUpdate` is introduced in this slice; the protocol is
pessimistic and consistent with progression.

## Consequences and limits

The lock prevents stale aggregate writes among participating supported writers.
It does not establish that all possible writers are safe: direct repository
writes and unsupported writers are outside this guarantee unless they first
join the same protocol. Database constraints may still be desirable as defense
in depth for child uniqueness. Lock timeouts and deadlock retries remain
unconfigured.
