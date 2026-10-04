# External Subject Ownership Lifecycle Authority

Status: VERIFY and INVALIDATE are canonical. REVERIFY is an implementation
candidate and is not canonical until independently reviewed and merged. Other
lifecycle mutations remain unimplemented.

## Authority boundary

The HARD-002 operation `SUBJECT_OWNERSHIP_MANAGE` authorizes administration of
external-subject ownership lifecycle state inside one exact namespace. It has
no source dimension. It is administrative authority only and is not proof of
ownership of `(namespace, externalId)`.

The only principal type supported by this foundation is an authenticated,
verified `WORKLOAD` principal with an exact grant for
`SUBJECT_OWNERSHIP_MANAGE` and the target namespace. The stable actor identity
comes from `AuthenticatedPrincipal.principalId` in the trusted server-side
security context. Future ownership history uses `WORKLOAD_OPERATOR` as
`actor_type` and the stable workload principal ID as `actor_id`.

Commands cannot supply actor type or actor ID. Credentials, credential IDs,
emails, display names, and external-subject locators are not audit actor IDs.

## Explicitly separate permissions

```text
PROGRESSION_EXECUTE
!= SUBJECT_PROVISION
!= SUBJECT_OWNERSHIP_MANAGE
```

No existing grant implies ownership-management authority. Namespace/source
authorization does not prove external ownership. AppUser-only authentication,
external self-link, and HARD-001 workload authentication alone do not grant
this authority.

AppUser operators and `SYSTEM` automation are unsupported. No grant, principal,
role, or trust entry is seeded by the authority foundation. No HTTP, message,
scheduled-job, or CLI ingress exists. Lifecycle mutations currently
implemented are VERIFY and INVALIDATE. The REVERIFY implementation candidate
is described below; it is not canonical before independent review and merge.
C2 execution enforcement and transfer remain outside this foundation.

## VERIFY slice

The initial lifecycle capability accepts only an existing external identity in
`ACTIVE / UNVERIFIED` state. It changes verification to `VERIFIED` and advances
`ownership_version` by one while preserving the locator, target, identity
class, and ownership status. `ACTIVE / VERIFIED` is an idempotent no-op with
no new event or version change. Disabled, revoked, invalidated, native, and
otherwise unsupported identities are rejected.

The command requires an expected ownership version, evidence type (trimmed,
nonblank, at most 64 characters), opaque evidence reference (trimmed,
nonblank, at most 255 characters), and reason (trimmed, nonblank, at most 512
characters). Evidence reference records operator evidence; it does not assert
cryptographic or third-party verification. Effective time is server-derived,
and recorded time is database-derived. The operation authorizes the exact
namespace before disclosing lookup results, locks the current identity row,
and atomically persists the state/version change with one immutable
`OWNERSHIP_VERIFIED` event using `LOGOS_OPERATOR_ACTION` provenance. This
slice adds no HTTP, message, scheduler, or CLI ingress and does not change
resolver or progression execution behavior.

## INVALIDATE slice

The canonical INVALIDATE slice supports only `EXTERNAL / ACTIVE / VERIFIED` to
`EXTERNAL / ACTIVE / INVALIDATED`. It preserves the identity, namespace,
external ID, target, identity class, and ACTIVE ownership status, and advances
`ownership_version` once. Other states, including native identities, are
rejected. An already active invalidated identity is an idempotent no-op,
including when the supplied expected version is stale; evidence inputs are
still validated before the no-op succeeds.

The command requires an expected ownership version and mandatory evidence
type, opaque evidence reference, and reason. They are trimmed, nonblank, and
limited to 64, 255, and 512 characters respectively. The operation uses the
namespace-scoped `SUBJECT_OWNERSHIP_MANAGE` authority and records the trusted
`WORKLOAD_OPERATOR` actor. It writes one immutable
`OWNERSHIP_VERIFICATION_INVALIDATED` event with `LOGOS_OPERATOR_ACTION`
provenance. Effective time is server-derived; recorded time is database-
derived. The current-state change and history event are atomic and use the
existing identity-row `PESSIMISTIC_WRITE` lock.

Re-verification, disable, reactivate, revoke, transfer, target correction, and
C2 enforcement remain unimplemented. No ingress or resolver/execution behavior
is added by this slice.

## REVERIFY implementation candidate

The proposed REVERIFY mutation is limited to
`EXTERNAL / ACTIVE / INVALIDATED` to `EXTERNAL / ACTIVE / VERIFIED`. It does not
accept `UNVERIFIED` (initial verification uses VERIFY), and it never accepts
native identities or the `logos-native` namespace. A real transition preserves
identity ID, namespace, external ID, target jogador ID, identity class, and
ownership status. It changes verification status and increments
`ownership_version` exactly once.

The caller must have a verified `WORKLOAD` principal with the exact
namespace-scoped `SUBJECT_OWNERSHIP_MANAGE` authorization. Authorization
precedes identity lookup disclosure. The audit actor is the trusted
`WORKLOAD_OPERATOR` derived from the authenticated principal ID; actor identity
is not command input. A real REVERIFY requires new mandatory evidence type,
opaque evidence reference, and reason. Existing normalization and maximum
lengths apply: trimmed, nonblank values up to 64, 255, and 512 characters,
respectively. The new evidence is appended to history and never overwrites
VERIFY or INVALIDATE evidence.

For a real mutation, the command requires the expected ownership version and
stale values conflict. Under the existing identity-row `PESSIMISTIC_WRITE`
lock, command handling proceeds in this order: authorize exact namespace,
lock and load the identity, validate evidence, detect a valid idempotent replay,
check expected version, apply the domain transition, and persist current state
and history atomically. If the identity is already `EXTERNAL / ACTIVE /
VERIFIED`, a syntactically valid REVERIFY request succeeds as a no-op even if
its expected version is stale. Evidence is still validated before this return.
The replay does not increment the version, append history, replace evidence,
or change timestamps.

A real transition appends exactly one immutable `OWNERSHIP_REVERIFIED` history
event at version N+1 with `LOGOS_OPERATOR_ACTION` provenance. It records the
before/after identity class (`EXTERNAL`), target (unchanged), ownership status
(`ACTIVE`), and verification status (`INVALIDATED` to `VERIFIED`), together
with the trusted actor, new evidence and reason. `effective_at` is
server-derived and `recorded_at` is database-derived. The existing row lock is
the serialization point; the state/version mutation and history insert share
one transaction, so history failure rolls back current state. Concurrent
equivalent requests produce one mutation and one event; the later lock holder
observes the valid already-verified replay.

No migration is required; the existing V48 schema supports the state, version,
event, and evidence fields. This candidate adds no ingress and does not change
resolver or progression execution behavior. REVERIFY is not canonical until
independently reviewed and merged. Disable, reactivate, revoke, transfer,
target correction, and C2 remain out of scope.
