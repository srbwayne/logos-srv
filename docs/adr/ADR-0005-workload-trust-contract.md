# ADR-0005 — Workload Trust Uses Asymmetric Signed Assertions

## Status

Accepted and frozen — HARD-001.

## Context

The AppUser JWT authenticates a human Logos account. The current human flow
uses an AppUser subject (email/username), symmetric HMAC signing, and
`LOGOS_JWT_SECRET`. It remains the human authentication mechanism; it is not a
workload identity and must not authenticate LifeOS-to-Logos requests.

## Decision

HARD-001 selects **Option D: asymmetric workload-signed assertion/JWT**. The
initial canonical workload principal is:

```text
principalType = WORKLOAD
principalId   = lifeos
```

LifeOS owns the private signing key. Logos trusts the corresponding public
key. Logos does not give LifeOS a key that permits signing as Logos users or
other principals. The workload principal is stable across credential/key
rotation; individual credential identity is not the workload identity.

Each outbound authenticated request will carry one assertion. The assertion
contract conceptually requires a trusted issuer, stable workload
subject/principal, Logos-specific audience, issued-at time, expiration, unique
assertion identity, credential/key identity, and signature integrity. Logos
will enforce bounded replay protection. A short lifetime alone is not
sufficient to prevent assertion replay.

Assertion replay protection is distinct from progression execution
idempotency. Progression idempotency remains keyed by `source +
idempotencyKey`; neither mechanism substitutes for the other.

## Logos trust registry

A Logos-controlled trust registry is conceptually required. It must be able to
represent workload principals, active public keys and key identities,
activation/revocation/rotation state, audit history, bounded overlap with
multiple active keys during rotation, individual key revocation, and whole
workload revocation. Trust establishment is controlled by Logos; LifeOS cannot
self-register arbitrary trusted keys.

## Alternatives not selected as the canonical model

The following are not HARD-001: human AppUser JWT for service authentication;
client ID plus shared secret; opaque API/service-key fallback; machine
account/password; symmetric service JWT; OAuth authorization server now; OIDC
provider now; or an mTLS requirement now. OAuth/OIDC and mTLS may be considered
as future evolutions, but they are not the frozen HARD-001 choice.

## Canonical technical profile

The HARD-001 architecture is frozen here. Its canonical workload assertion
technical profile is frozen in
[ADR-0009 — Workload Assertion Technical Profile](ADR-0009-workload-assertion-technical-profile.md).
ADR-0009 specifies the algorithm, JOSE headers and claims, issuer and
audience, lifetime and clock skew, key lookup and formats, replay semantics,
trust-registry direction, rotation/revocation behavior, transport, and
authentication failure contract.

That profile does **not** authorize implementation. Production authentication,
migrations, runtime changes, deployment, or changes to LifeOS remain outside
the authorization granted by these ADRs and require their own approval.

## Separation from authorization

HARD-001 answers only **who authenticated?** Successful workload
authentication does not authorize a source, namespace, operation, progression
execution, subject provisioning, exact read, or history read. Those decisions
belong to HARD-002.
