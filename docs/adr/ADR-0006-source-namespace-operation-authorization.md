# ADR-0006 — Workload Authorization Is Logos-Managed Relational Policy

## Status

Accepted and frozen — HARD-002. Depends on HARD-001.

## Authorization input

HARD-002 receives the normalized, already-authenticated principal produced by
HARD-001, conceptually:

```text
principalType        = WORKLOAD
principalId          = lifeos
authenticationMethod = ASYMMETRIC_SIGNED_ASSERTION
credentialIdentity   = verified key identity
authenticationStatus = VERIFIED
```

HARD-002 does not parse raw JWTs, verify signatures, parse keys, or repeat
authentication. Policy ownership is keyed by stable `principalType +
principalId`, not by `credentialIdentity`, so key rotation does not change
grants. Source and namespace are not trusted merely because they appeared in a
signed assertion or request; they remain authorization inputs.

## Decision

Logos owns a persistent relational authorization registry. Grants model these
dimensions separately:

* operation;
* source;
* namespace.

Every applicable dimension must be explicitly authorized. Policy is **default
deny**: absence of an explicit grant denies the operation.

The initial LifeOS grant is exactly:

```text
principal:  WORKLOAD / lifeos
operation:  PROGRESSION_EXECUTE
source:     lifeos
namespace:  lifeos
```

This does not automatically grant `PROGRESSION_EXECUTION_READ`,
`PROGRESSION_HISTORY_READ`, `SUBJECT_PROVISION`, administrative operations,
or any other capability. Each future capability requires its own explicit
grant. In particular, `PROGRESSION_EXECUTE` never implies read, history, or
provisioning authority.

## Authorization is not ownership

Namespace authorization means that a principal may perform an authorized
operation in that namespace. It does not mean the principal owns every
`externalId` in that namespace. The HARD-002 authorization registry must not
become an implicit ownership registry; external subject ownership belongs to
HARD-003.

## Open implementation-plan decisions

This ADR does not choose physical tables/schema, Spring annotations, admin
endpoints, error-code conventions, human AppUser authorization policy for
integration operations, or an audit API. Those belong to a later implementation
plan.
