# External Subject Ownership Lifecycle Authority

Status: VERIFY, INVALIDATE, and REVERIFY are canonical. DISABLE, REACTIVATE,
REVOKE, TRANSFER, REASSIGNMENT, TARGET_CORRECTION, and C2 enforcement remain
unimplemented.

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
scheduled-job, or CLI ingress exists. Canonical lifecycle mutations are VERIFY,
INVALIDATE, and REVERIFY. DISABLE, REACTIVATE, REVOKE, TRANSFER, REASSIGNMENT,
TARGET_CORRECTION, and C2 execution enforcement remain unimplemented and
outside this foundation.

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

Disable, reactivate, revoke, transfer, reassignment, target correction, and C2
enforcement remain unimplemented. No ingress or resolver/execution behavior
is added by this slice.

## REVERIFY implementation

The canonical REVERIFY mutation is limited to
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
event, and evidence fields. REVERIFY adds no ingress and does not change
resolver or progression execution behavior. Disable, reactivate, revoke,
transfer, reassignment, target correction, and C2 remain unimplemented and out
of scope.

## DISABLE design — unimplemented

The designed DISABLE transition changes only ownership status:

```text
EXTERNAL / ACTIVE / UNVERIFIED  -> EXTERNAL / DISABLED / UNVERIFIED
EXTERNAL / ACTIVE / VERIFIED    -> EXTERNAL / DISABLED / VERIFIED
EXTERNAL / ACTIVE / INVALIDATED -> EXTERNAL / DISABLED / INVALIDATED
```

Ownership status and verification status are independent. A real DISABLE
preserves identity ID, namespace, external ID, target jogador ID, identity
class, and verification status; it changes `ACTIVE` to `DISABLED` and advances
`ownership_version` exactly once. External `NOT_REQUIRED` is not eligible:
V47's class/namespace constraint requires non-native `EXTERNAL` identities to
have a verification status other than `NOT_REQUIRED`. Native identities and
the `logos-native` namespace are rejected.

The future command contains `ExternalSubjectReference`,
`expectedOwnershipVersion`, and a mandatory reason. Reason is trimmed,
nonblank, and limited to 512 characters, matching the history column. Evidence
type/reference are not required and are stored as NULL. The verification
evidence value object is not reused because it requires proof-specific fields;
DISABLE must not fabricate verification evidence. Actor fields and timestamps
are not command input.

The operation reuses exact-namespace `SUBJECT_OWNERSHIP_MANAGE`, authorizes
before lookup disclosure, and accepts the trusted verified WORKLOAD operator
context only. History actor is `WORKLOAD_OPERATOR`, derived from the server-side
principal ID, with `LOGOS_OPERATOR_ACTION` provenance. For a real transition,
the expected version must match the locked state; otherwise the existing
ownership version conflict applies. A valid already-disabled external identity
with verification status `UNVERIFIED`, `VERIFIED`, or `INVALIDATED` is an
idempotent replay after reason validation. It ignores stale expected version
and performs no mutation, version increment, history append, reason
replacement, or timestamp change.

A real transition appends one immutable `OWNERSHIP_DISABLED` event at version
N+1 with the full before/after identity class, target, ownership status, and
verification status snapshot. Evidence fields are NULL; the mandatory reason
is recorded. `effective_at` is server-derived and `recorded_at` is
database-derived. The event fits V47's `VARCHAR(64)` nonblank-only event
constraint; no event enum, allowlist, or trigger restriction exists. Ownership
status admits `DISABLED`; the reason column is nullable `VARCHAR(512)` and the
evidence columns are nullable. Therefore V49 is not required.

The existing identity-row `PESSIMISTIC_WRITE` lock serializes this mutation.
Current state/version and history append commit atomically; history failure
rolls back the current-row mutation. Concurrent equivalent requests yield one
transition/event and a successful replay for the later lock holder.

DISABLE is reversible in principle and preserves the verification state and
identity data needed by a future REACTIVATE. REACTIVATE is not designed here.
DISABLE is not REVOKE and must not substitute for it; REVOKE terminality remains
unfrozen. DISABLE changes no resolver or progression execution behavior and
adds no ingress. Any future active/usable execution requirement belongs to a
separate C2 gate. DISABLE remains unimplemented.
