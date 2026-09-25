# Progression Execution HTTP V1 — Durable Progression Execution

Status: CURRENT — first supported progression HTTP surface.

## Identity

An external execution is identified by `source + idempotencyKey`. `source` is
trimmed, lower-cased with `Locale.ROOT`, and limited to 64 characters.
`idempotencyKey` is trimmed, case-sensitive, and limited to 255 characters.
This identity is distinct from both the external subject and configuration
identity.

## HTTP contract

The idempotent boundary is:

```http
POST /api/internal/v1/progression/executions
```

The durable execution resource also exposes:

```http
GET /api/internal/v1/progression/executions
    ?sourceSystem={sourceSystem}&idempotencyKey={idempotencyKey}

GET /api/internal/v1/progression/executions/history
    ?subjectNamespace={namespace}&subjectExternalId={externalId}
    &page={page}&size={size}
```

The POST request carries the external subject in the body:

```json
{
  "subject": {"namespace": "lifeos", "externalId": "user-123"},
  "execution": {"source": "lifeos", "idempotencyKey": "reading-session-123"},
  "configuration": {"key": "reading", "revision": 3},
  "details": [{"factorKey": "pages_read", "value": 30}]
}
```

This V1 execution resource is the first supported progression HTTP contract.
The earlier progression V1/V2/V3 evaluate routes were pre-release experiments
and are not supported compatibility surfaces.

Compatible additions remain on V1. Flyway migration versions, internal task
numbers, and persistence evolution do not increment the HTTP major version.
A future V2 requires a breaking change to an already supported HTTP contract.

## Processing semantics

The application computes a canonical request fingerprint from the execution
identity, subject reference, requested configuration reference, and ordered
progression details. Raw JSON is not used. Detail order is retained because
the existing fact contract is not reinterpreted by the idempotency boundary.

For a new identity, the service resolves the subject and configuration/policy
pair, reserves the identity, executes progression, and stores the serialized
outcome in one transaction. The database unique constraint is the concurrency
guard; a pre-check alone is not relied upon.

For a repeated identity:

* the same fingerprint returns the stored original outcome with HTTP 200 and
  does not execute progression again;
* a different fingerprint returns HTTP 409 and does not mutate progression.

If processing fails, the transaction rolls back the reservation, so a retry
can execute normally. If the response is lost after commit, the retry reads
the stored outcome and the original configuration and skill-policy version
references remain attached to that execution.

## Characterized concurrency

PostgreSQL characterization tests cover the supported durable progression path:

* the same identity and fingerprint produce one mutation; concurrent callers
  receive the original outcome or its replay;
* the same identity with a different fingerprint has one winner and the other
  caller receives HTTP 409 without a second mutation;
* different identities for one subject serialize through that subject's
  `Jogador` row lock and preserve both deltas;
* different subjects progress independently through separate row locks.

These guarantees apply to the durable progression boundary. The supported
shared-state writers also have a separate, characterized consistency boundary:
they acquire the same row-level `PESSIMISTIC_WRITE` lock on `Jogador` before
reading or mutating shared state. Profile and nickname updates, Registro Vicio
creation, and Registro Vicio processing participate; read-only player queries
remain unlocked.

PostgreSQL characterization of the real Registro Vicio services additionally
proves that two concurrent supported creates for one Jogador/Vicio preserve
one logical `VicioJogador` while creating two `RegistroVicio` rows. It also
proves that two concurrent supported processings for one Jogador/Debuff
preserve one logical `DebuffJogador` and serially accumulate potency from the
configured penalty of 7 to 14. Neither scenario produced a duplicate child,
lost accumulation, deadlock, or timeout.

This is a guarantee for supported shared-state writers, not a universal claim
about every possible writer. Unsupported or direct repository writes are
outside the `Jogador` lock guarantee unless they explicitly participate in the
same protocol. The complete writer decision and its limitations are recorded
in `docs/adr/ADR-0004-jogador-write-serialization.md`.

## Persistence

V34 creates `progression_external_execution` with a unique
`(source_system, idempotency_key)` constraint. It stores the request identity,
request fingerprint, subject/configuration references, resolved
`configuration_version_id` and `skill_policy_version_id`, serialized outcome,
and creation time. Existing `RegistroAtividade` rows remain legacy records and
do not receive invented source or idempotency identities.

## Security and ownership

The existing authentication configuration is unchanged. Source authorization
is intentionally not implemented: an authenticated caller can currently state
a source value, so service-to-service authentication and source/namespace
authorization remain prerequisites for production LifeOS rollout.

LifeOS remains the canonical owner of the source fact. Logos remains the
canonical owner of progression state. Idempotency protects delivery of the
external execution; it does not turn the source event into a Logos-owned fact.

## Remaining debts

Durable LifeOS delivery/outbox, service-to-service authentication, source and
namespace authorization, external identity proof, operational/historical
replay policy beyond idempotent retrieval of a stored execution outcome, and
version-authoring lifecycle remain separate follow-up work.
POC self-provisioning is documented in `PROGRESSION-SUBJECT-IDENTITY.md`; it is
not a production service-trust or ownership solution.

The serialized outcome may also contain additive Attribute semantic identity
metadata captured when the execution completes. Replays, exact reads, and
history return that original snapshot; they do not resolve semantic keys from
the live Attribute catalog. Older outcomes without this metadata remain
readable and expose a nullable semantic key.
