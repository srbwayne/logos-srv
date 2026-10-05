# External Subject Ownership Lifecycle Authority

Status: VERIFY, INVALIDATE, REVERIFY, DISABLE, and REACTIVATE are canonical.
REVOKE is designed but unimplemented. TRANSFER, REASSIGNMENT,
TARGET_CORRECTION, and C2 enforcement are unimplemented and not designed by
this gate.

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
INVALIDATE, REVERIFY, DISABLE, and REACTIVATE. REVOKE is designed but
unimplemented. TRANSFER, REASSIGNMENT, TARGET_CORRECTION, and C2 execution
enforcement remain unimplemented and outside this foundation.

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

REACTIVATE is canonical as documented below. REVOKE is designed but
unimplemented. Transfer, reassignment, target correction, and C2 enforcement
remain unimplemented and not designed by this gate. No ingress or
resolver/execution behavior is added by these lifecycle capabilities.

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
resolver or progression execution behavior. Reactivate, revoke, transfer,
reassignment, target correction, and C2 remain unimplemented and out of scope.

## DISABLE implementation — canonical

The previously designed DISABLE mutation became canonical when PR #60 was
squash-merged as `64f4f6bd71cb5210f1ecfd9c64b4d0524e0e7909`. Post-merge CI run
#186 passed. The CI recovery included
`-Dspring.test.context.cache.maxSize=2` in the test workflow; this is test-
runtime infrastructure and does not change DISABLE semantics.

The canonical DISABLE transition changes only ownership status:

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

The command contains `ExternalSubjectReference`,
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
evidence columns are nullable. The migration head remains V48; V49 is not required
or created.

The existing identity-row `PESSIMISTIC_WRITE` lock serializes this mutation.
Current state/version and history append commit atomically; history failure
rolls back the current-row mutation. Concurrent equivalent requests yield one
transition/event and a successful replay for the later lock holder.

DISABLE is reversible in principle and preserves verification state and
identity data. REACTIVATE is designed below but remains unimplemented. DISABLE
is not REVOKE and must not substitute for it; REVOKE terminality remains
unfrozen. DISABLE changes no resolver or progression execution behavior and
adds no ingress. Any future active/usable execution requirement belongs to a
separate C2 gate.


## REACTIVATE design — designed, unimplemented

REACTIVATE is the administrative inverse of DISABLE and changes ownership
status only. Its exact transitions are:

```text
EXTERNAL / DISABLED / UNVERIFIED  -> EXTERNAL / ACTIVE / UNVERIFIED
EXTERNAL / DISABLED / VERIFIED    -> EXTERNAL / ACTIVE / VERIFIED
EXTERNAL / DISABLED / INVALIDATED -> EXTERNAL / ACTIVE / INVALIDATED
```

A real transition is eligible only for a non-native `EXTERNAL` identity in
`DISABLED` ownership state with verification status `UNVERIFIED`, `VERIFIED`,
or `INVALIDATED`. The `logos-native` namespace, `LOGOS_NATIVE`, `REVOKED`,
`NOT_REQUIRED` for an external identity, and all unsupported combinations are
rejected; REACTIVATE does not repair invalid state. It preserves identity ID,
namespace, external ID, target jogador ID, identity class, and verification
status. It changes `DISABLED` to `ACTIVE` and increments
`ownership_version` from N to N+1 exactly once. It never verifies, invalidates,
or reverifies ownership. In particular, a disabled invalidated identity
returns as `ACTIVE / INVALIDATED`. This status transition does not define
resolver or progression execution behavior; an active mapping is not thereby
declared executable, and C2 remains separate.

The future `ReactivateExternalSubjectOwnershipCommand` contains only the
external subject reference, expected ownership version, and mandatory
administrative reason. The reason is trimmed, nonblank, and at most 512
characters. It contains no actor, target, status, evidence, or timestamp
fields. REACTIVATE is not proof of ownership: `evidence_type` and
`evidence_reference` are NULL, and prior history is not changed.

The operation reuses exact-namespace `SUBJECT_OWNERSHIP_MANAGE` authorization
for an authenticated, verified `WORKLOAD` principal. Authorization occurs
before identity lookup disclosure. The immutable audit actor is
`WORKLOAD_OPERATOR`, derived from the trusted server-side principal ID; actor
fields are not command input. No authorization operation is added.

For a real transition, the expected ownership version must match the locked
current version or the request conflicts. A valid already-active
`EXTERNAL` identity with verification status `UNVERIFIED`, `VERIFIED`, or
`INVALIDATED` is an idempotent `ensure ACTIVE` replay after reason validation.
It ignores a stale expected version and makes no state, version, history,
reason, or timestamp change. This replay does not establish that the identity
was previously disabled or infer historical provenance. Unsupported active
shapes are not replay-valid. Processing order is authorization,
`PESSIMISTIC_WRITE` lookup, not-found handling, reason validation, valid
replay detection, eligible disabled-state validation, expected-version check,
domain transition, and atomic current-state/history persistence.

A real transition appends exactly one immutable
`OWNERSHIP_REACTIVATED` event at aggregate version N+1 with
`LOGOS_OPERATOR_ACTION` provenance. Its complete snapshot records `EXTERNAL`
before and after; the unchanged target before and after; `DISABLED` to `ACTIVE`;
and the unchanged eligible verification status. It also records the trusted
`WORKLOAD_OPERATOR` actor, NULL evidence fields, mandatory normalized reason,
server-derived `effective_at`, and database-derived `recorded_at`. Existing
identity-row `PESSIMISTIC_WRITE` locking serializes the operation. State/version
and history must commit in one transaction; history failure leaves ownership
`DISABLED` and the version unchanged. Concurrent equivalent requests produce
one mutation, one version increment, and one event; the later lock holder
succeeds through the valid already-active replay.

The physical schema supports this design without a migration. V47 stores
`ownership_status` and `verification_status` as `VARCHAR(32)` with checks that
allow `ACTIVE`, `DISABLED`, `UNVERIFIED`, `VERIFIED`, and `INVALIDATED`; its
class/namespace check excludes `NOT_REQUIRED` for external identities. The
history `event_type` is `VARCHAR(64)` with only a nonblank check, so
`OWNERSHIP_REACTIVATED` is accepted; no enum or event allowlist exists. The
append-only triggers reject UPDATE, DELETE, and TRUNCATE, not INSERT of a new
event. `reason` is `VARCHAR(512)`, both evidence columns are nullable, and
`recorded_at` is database-defaulted. V48 changes authorization constraints
only. Migration head remains V48; V49 is not required.

REACTIVATE is canonical and implemented. REVOKE is distinct and is designed
below, but remains unimplemented. TRANSFER, REASSIGNMENT, TARGET_CORRECTION,
and C2 remain unimplemented and are not designed by this gate.

## REVOKE design — designed, unimplemented

REVOKE terminates the current external ownership binding. It is stronger than
the reversible DISABLE/REACTIVATE pair and is terminal for this binding's
lifecycle. It does not decide whether a separately authorized future operation
may create a replacement binding, transfer ownership, reassign a target, or
correct a target. No unrevoke or automatic rebinding operation is defined.

The only real transitions are:

```text
EXTERNAL / ACTIVE   / UNVERIFIED  -> EXTERNAL / REVOKED / UNVERIFIED
EXTERNAL / ACTIVE   / VERIFIED    -> EXTERNAL / REVOKED / VERIFIED
EXTERNAL / ACTIVE   / INVALIDATED -> EXTERNAL / REVOKED / INVALIDATED
EXTERNAL / DISABLED / UNVERIFIED  -> EXTERNAL / REVOKED / UNVERIFIED
EXTERNAL / DISABLED / VERIFIED    -> EXTERNAL / REVOKED / VERIFIED
EXTERNAL / DISABLED / INVALIDATED -> EXTERNAL / REVOKED / INVALIDATED
```

Only an `EXTERNAL` identity outside the `logos-native` namespace with
`ACTIVE` or `DISABLED` ownership and verification status `UNVERIFIED`,
`VERIFIED`, or `INVALIDATED` is eligible. Native identities, the native
namespace, `NOT_REQUIRED`, `REVOKED` as a real-transition source, and all
unsupported combinations are rejected. REVOKE preserves identity ID,
namespace, external ID, target jogador ID, identity class, and verification
status. It changes ownership status to `REVOKED` and increments
`ownership_version` from N to N+1 exactly once. It does not verify, invalidate,
or reverify.

The future `RevokeExternalSubjectOwnershipCommand` contains only the external
subject reference, expected ownership version, and mandatory administrative
reason. The reason is trimmed, nonblank, and at most 512 characters; the
existing `SubjectOwnershipAdministrativeReason` is the intended value object.
No actor, target, status, evidence, or timestamp is command input. REVOKE is
not verification proof: history `evidence_type` and `evidence_reference` are
NULL. Audit uses `LOGOS_OPERATOR_ACTION` and the trusted server-derived
`WORKLOAD_OPERATOR` principal ID from a verified workload context.

Authorization reuses `SUBJECT_OWNERSHIP_MANAGE` for the exact namespace and
must precede identity lookup disclosure. Processing order is authorization,
locked lookup, not-found handling, mandatory reason validation, supported
already-revoked replay detection, eligible ACTIVE/DISABLED source validation,
expected-version validation, domain transition, and atomic persistence. A real
transition requires the expected version to equal the locked version; a stale
version conflicts. A supported `EXTERNAL / REVOKED` identity with verification
status `UNVERIFIED`, `VERIFIED`, or `INVALIDATED` is an idempotent ensure-
revoked replay after reason validation. Replay ignores stale expected version
and performs no mutation, version increment, history append, reason
replacement, or timestamp replacement. It does not rewrite or replace the
original revocation reason. Unsupported revoked shapes are not replay-valid.

A real transition appends exactly one immutable `OWNERSHIP_REVOKED` history
event at aggregate version N+1 with `LOGOS_OPERATOR_ACTION` provenance. Its
complete before/after snapshot records `EXTERNAL` identity class and the same
target and verification status on both sides; ownership moves from `ACTIVE`
or `DISABLED` to `REVOKED`. It records the trusted `WORKLOAD_OPERATOR` actor,
NULL evidence fields, normalized reason, server-derived `effective_at`, and
database-derived `recorded_at`.

REVOKE reuses the identity-row `PESSIMISTIC_WRITE` lock. Current state/version
and the single history append must commit in one transaction; a history failure
leaves the source status and version unchanged. Equivalent concurrent requests
serialize to one transition, one version increment, and one
`OWNERSHIP_REVOKED` event; the later lock holder succeeds as a valid replay.

V47's ownership check permits `REVOKED`, its class/namespace constraint
excludes `NOT_REQUIRED` for external identities, and its event type is
nonblank `VARCHAR(64)` without an event-name allowlist. The reason column is
`VARCHAR(512)`, evidence columns are nullable, `recorded_at` is
database-defaulted, and append-only triggers prohibit UPDATE, DELETE, and
TRUNCATE while allowing new history inserts. V48 changes authorization
constraints only. The design requires no migration; the head remains V48 and
V49 is not required.

REVOKE is a state transition, never hard deletion. `REVOKED` is terminal for
the current binding, and REACTIVATE continues to reject it. No runtime resolver
or progression execution behavior is specified or changed here; C2 remains
unimplemented and must separately determine how ownership status affects
execution eligibility. TRANSFER, REASSIGNMENT, TARGET_CORRECTION, and C2 remain
unimplemented and undesigned by this gate. REVOKE itself remains a design only,
not canonical or implemented.
