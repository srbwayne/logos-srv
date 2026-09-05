# Progression Execution Idempotency

Status: IMPLEMENTED for the V3 external execution boundary.

## Identity

An external execution is identified by `source + idempotencyKey`. `source` is
trimmed, lower-cased with `Locale.ROOT`, and limited to 64 characters.
`idempotencyKey` is trimmed, case-sensitive, and limited to 255 characters.
This identity is distinct from both the external subject and configuration
identity.

## HTTP contract

The idempotent boundary is:

```http
POST /api/internal/v3/progression/external/{namespace}/{externalId}/evaluate
```

Example request:

```json
{
  "execution": {
    "source": "lifeos",
    "idempotencyKey": "reading-session-123"
  },
  "configuration": {
    "key": "reading"
  },
  "details": [
    { "factorKey": "pages_read", "value": 30 }
  ]
}
```

The V1 and V2 contracts remain unchanged. V3 does not expose internal
configuration, subject, or skill-policy IDs.

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
namespace authorization, provisioning, replay, general same-subject
concurrency, and version-authoring lifecycle remain separate follow-up work.
