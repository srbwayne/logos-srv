# ADR-0007 — External Subject Ownership Uses Stateful Lifecycle

## Status

Accepted and frozen — HARD-003, **Option B: stateful ownership registry**.
Depends on HARD-001 and HARD-002.

## Boundary

`(namespace, externalId)` is an external locator, not proof of ownership.
Successful HARD-001 authentication proves workload identity, not external
subject ownership. Successful HARD-002 namespace authorization proves
permission to perform a granted operation in a namespace, not ownership of an
external subject in it.

## Decision

External subject ownership uses a stateful ownership registry. Bindings are
immutable by default. Lifecycle must conceptually support `ACTIVE`,
`DISABLED`/`REVOKED`, and `TRANSFERRED`/`REASSIGNED`, with tombstone or
inactive historical state when required. These are conceptual states; this
ADR does not freeze physical enum or column names.

Initial authoritative creation/correction is controlled by a Logos operator
path. Any future human self-link requires external proof. AppUser self-target
alone is not external ownership proof. The initial HARD-002 LifeOS
execute-only grant does not include subject provisioning.

For the same external reference and same valid target, confirmation is
idempotent. The same reference with a different target conflicts by default.
Silent overwrite, delete-then-reclaim, and automatic takeover are prohibited.

Any supported transfer or correction requires explicit authority and preserves
prior ownership, current ownership, history, and reason/audit context. Disable
and revocation state is independent of HARD-002 authorization. New progression
mutation requires both authorization and an active/usable mapping. Ownership
changes must not rewrite historical durable execution snapshots.

Ordinary hard deletion of ownership-bearing mappings is prohibited as the
canonical direction. Preserve provenance/history through tombstones or
immutable historical ownership records selected by a future technical plan.

The `logos-native` namespace remains Logos-controlled. External workloads and
human ownership flows cannot claim, transfer, delete, or take over native
mappings.

## C1B ownership-management authority foundation

The authority foundation defines the distinct HARD-002 operation
`SUBJECT_OWNERSHIP_MANAGE`, scoped to one namespace and not to a source. It is
administrative authority only: it does not prove external ownership or imply
`PROGRESSION_EXECUTE`. Existing `PROGRESSION_EXECUTE` and `SUBJECT_PROVISION`
grants do not imply ownership-management authority.

This foundation supports only a verified `WORKLOAD` principal whose stable
principal ID is derived from the trusted server-side security context and
whose exact namespace grant is evaluated by HARD-002. Immutable lifecycle
history will use actor type `WORKLOAD_OPERATOR` and that principal ID; actor
fields are never command input. AppUser and `SYSTEM` operators remain
unsupported. V48 changes authorization constraints only and creates no
grants, principals, or trust records. It introduced no ingress. The canonical
C1B lifecycle mutations are VERIFY, INVALIDATE, REVERIFY, and DISABLE.
REACTIVATE is designed but unimplemented. REVOKE, TRANSFER, REASSIGNMENT,
TARGET_CORRECTION, and C2 enforcement remain unimplemented and are not designed
by this gate.

## Audit, lifecycle, and concurrency requirements

The architecture must preserve enough information for ownership history,
actor principal, effective timestamps, transfer/correction history,
disable/revoke history, and a proof/evidence reference where applicable. The
initial persistence schema is defined by the canonical C1A migration.

A future implementation must establish atomic semantics for: same reference
and same target; same reference and different target; claim versus disable;
claim versus transfer; and transfer versus execution. This documentation
freeze does not implement those semantics.

## C1A approved physical foundation

The approved C1A foundation adds current classification metadata to the
existing `progression_subject_identity` row, which remains the single current
locator/target/state row. It records identity class, ownership status,
verification status, and ownership version. Initial ownership history is
stored separately and append-only; provenance, actor, evidence, reason, and
historical times are not duplicated on the current row.

The initial state vocabulary is `ACTIVE` / `DISABLED` / `REVOKED` for
ownership and `NOT_REQUIRED` / `UNVERIFIED` / `VERIFIED` / `INVALIDATED` for
verification. C1A only establishes initial classifications and creation
history; it adds no lifecycle commands and does not enforce ownership during
progression execution. Resolver behavior remains unchanged until a separately
authorized C2 slice.

Legacy native mappings are classified `LOGOS_NATIVE / ACTIVE / NOT_REQUIRED`
with `LEGACY_LOGOS_NATIVE_UNKNOWN`; legacy external mappings are
`EXTERNAL / ACTIVE / UNVERIFIED` with `LEGACY_EXTERNAL_UNKNOWN`. Existing
actor, evidence, and effective time remain unknown. New native registration
uses `LOGOS_NATIVE_REGISTRATION`; the new external POC self-link uses
`POC_SELF_LINK`, with the authenticated AppUser recorded only as an action
actor and never as ownership proof. The `logos-native` namespace remains
Logos-controlled. These physical C1A decisions do not authorize C1B, C2, or
operational activation.

## C1B VERIFY implementation slice

The first lifecycle mutation is limited to `EXTERNAL / ACTIVE / UNVERIFIED` to
`EXTERNAL / ACTIVE / VERIFIED`. It requires the namespace-scoped
`SUBJECT_OWNERSHIP_MANAGE` authority and records a `WORKLOAD_OPERATOR` actor
from trusted server context. The command supplies mandatory opaque evidence
type/reference and reason; effective time is server-derived. A row-level
`PESSIMISTIC_WRITE` lock serializes the current identity, and current state plus
one append-only `OWNERSHIP_VERIFIED` history event commit atomically at the
incremented ownership version. An already active verified identity is an
idempotent no-op. No other lifecycle transition, ingress, or execution
enforcement is included.

## C1B INVALIDATE implementation slice

The INVALIDATE mutation is limited to `EXTERNAL / ACTIVE / VERIFIED` to
`EXTERNAL / ACTIVE / INVALIDATED`. It preserves identity, locator, target,
identity class, and ACTIVE ownership status, and advances `ownership_version`
once. Native identities and unsupported states are rejected. An already
active invalidated identity is an idempotent no-op, including for a stale
expected version, after mandatory evidence syntax is validated.

The command requires an expected version, trimmed nonblank evidence type
(maximum 64 characters), opaque evidence reference (maximum 255 characters),
and reason (maximum 512 characters). It authorizes the exact namespace using
`SUBJECT_OWNERSHIP_MANAGE`; the audit actor is derived from the trusted
`WORKLOAD_OPERATOR` context. The immutable
`OWNERSHIP_VERIFICATION_INVALIDATED` event uses `LOGOS_OPERATOR_ACTION`
provenance. Effective time is server-derived and recorded time is
database-derived. Current state/version and the history entry are committed
atomically under the existing identity-row `PESSIMISTIC_WRITE` lock.

INVALIDATE adds no migration or ingress and does not change resolver or
progression execution behavior. Reactivate, revoke, transfer, reassignment,
target correction, and C2 enforcement remain unimplemented.

## C1B REVERIFY implementation

REVERIFY is a separate mutation limited to
`EXTERNAL / ACTIVE / INVALIDATED` to `EXTERNAL / ACTIVE / VERIFIED`. Initial
`UNVERIFIED` identities continue to use VERIFY; native identities, including
the `logos-native` namespace, are never eligible. The operation preserves the
identity ID, locator, target, `EXTERNAL` identity class, and `ACTIVE` ownership
status. A real transition changes only verification status and advances
`ownership_version` from N to N+1.

REVERIFY reuses the namespace-scoped `SUBJECT_OWNERSHIP_MANAGE` capability and
the verified `WORKLOAD` operator context. Authorization must precede lookup
disclosure. The immutable audit actor is `WORKLOAD_OPERATOR`, derived from the
trusted authenticated principal ID; commands contain no actor fields.
REVERIFY requires new, mandatory evidence type, opaque evidence reference,
and reason, normalized and bounded by the existing
`OwnershipVerificationEvidence` rules (64, 255, and 512 characters). It
appends that evidence to a new history event; it never replaces or edits the
evidence for VERIFY or INVALIDATE.

For a real transition, `expectedOwnershipVersion` must equal the locked current
version. An already `EXTERNAL / ACTIVE / VERIFIED` identity is a valid
idempotent REVERIFY replay: validate mandatory evidence syntax first, then
return without checking the expected version, changing evidence or timestamps,
incrementing the version, or appending history. A real stale-version command
conflicts. The command order is authorization, pessimistic locked lookup,
evidence construction/validation, valid replay detection, expected-version
check and domain transition, then persistence.

A real transition appends exactly one immutable `OWNERSHIP_REVERIFIED` event at
aggregate version N+1 with `LOGOS_OPERATOR_ACTION` provenance. Its snapshot
records `EXTERNAL` before and after, the unchanged target before and after,
`ACTIVE` ownership before and after, and `INVALIDATED` to `VERIFIED`; it also
records the trusted workload actor, new evidence and reason, server-derived
`effective_at`, and database-derived `recorded_at`. The existing
`progression_subject_identity` `PESSIMISTIC_WRITE` lock remains the
serialization point. Current state/version and the history append must commit
in one transaction; a history failure rolls back the current mutation. Under
concurrent equivalent requests, one performs the transition and appends the
event, while the next observes `ACTIVE / VERIFIED` under the lock and succeeds
as the no-op replay.

This canonical slice needs no migration: V48 provides the current verification
state/version and append-only history fields, and the event name fits the
existing event-type column. REVERIFY adds no ingress, grants, principals, trust
records, workload activation, resolver behavior, or execution enforcement.
REACTIVATE is designed but unimplemented. REVOKE, TRANSFER, REASSIGNMENT,
TARGET_CORRECTION, and C2 enforcement remain unimplemented and are not designed
by this gate.

## C1B DISABLE implementation — canonical

The previously designed DISABLE mutation became canonical when PR #60 was
squash-merged as `64f4f6bd71cb5210f1ecfd9c64b4d0524e0e7909`. Post-merge CI run
#186 passed. The CI recovery included
`-Dspring.test.context.cache.maxSize=2` in the test workflow; this is test-
runtime infrastructure and does not change DISABLE semantics.

DISABLE is an ownership-status mutation orthogonal to verification.
The only eligible source states are `EXTERNAL / ACTIVE / UNVERIFIED`,
`EXTERNAL / ACTIVE / VERIFIED`, and `EXTERNAL / ACTIVE / INVALIDATED`; each
transitions to `EXTERNAL / DISABLED` while preserving its verification status.
V47's class/namespace constraint excludes `NOT_REQUIRED` for external
identities. `LOGOS_NATIVE` identities and the `logos-native` namespace are
never eligible. DISABLE does not verify, invalidate, reverify, revoke, transfer,
reassign, or change the locator or target.

`DisableExternalSubjectOwnershipCommand` contains the external subject
reference, expected ownership version, and mandatory reason. It contains no
actor, target, state, or timestamp fields. The reason is trimmed, nonblank,
and at most 512 characters, matching the history column bound. The current
`OwnershipVerificationEvidence` value object is not reused: its required
evidence type and reference are specific to verification evidence and are not
required for this administrative state change. History stores
`evidence_type = NULL` and `evidence_reference = NULL`; no synthetic evidence
is created. The reason is stored in the existing nullable `reason VARCHAR(512)`
column.

The operation reuses exact-namespace `SUBJECT_OWNERSHIP_MANAGE` authorization
and requires the verified `WORKLOAD` operator context. Authorization precedes
identity lookup disclosure. The immutable actor is `WORKLOAD_OPERATOR`, with
its ID derived from the trusted principal context. Provenance is
`LOGOS_OPERATOR_ACTION`. A real transition requires the expected ownership
version to match the locked current version and increments it once. An already
`EXTERNAL / DISABLED` identity with verification state `UNVERIFIED`, `VERIFIED`,
or `INVALIDATED` is a valid replay after reason validation; stale expected
versions are ignored and the replay changes no state, version, history, reason,
or timestamps. Other identity/state combinations are not replay-valid.

A real transition appends one immutable `OWNERSHIP_DISABLED` history event at
aggregate version N+1. It records `EXTERNAL` before and after; the unchanged
target before and after; `ACTIVE` to `DISABLED`; and the same eligible
verification status before and after. It records the trusted actor, mandatory
reason, null evidence fields, server-derived `effective_at`, and
database-derived `recorded_at`. V47 defines `event_type` as `VARCHAR(64)` with
only a nonblank check; it has no event enum, event allowlist, or trigger
restriction. The value fits the existing column. Ownership status already
allows `DISABLED`, reason fits the existing column, and evidence columns are
nullable. No migration is required.

The current identity row's existing `PESSIMISTIC_WRITE` lock remains the
serialization point. State/version mutation and one history append commit in
the same transaction; history failure rolls the current row back. Concurrent
equivalent DISABLE calls produce one mutation and one event, while the later
lock holder observes the disabled state and succeeds as a validated replay.

DISABLE is reversible in principle and preserves verification status, target,
locator, identity class, and history. REACTIVATE is designed in the following
section but remains unimplemented. DISABLE is not REVOKE and must not be used
as its substitute; REVOKE terminality remains unfrozen. DISABLE does not change
resolver behavior, progression execution authorization/enforcement, or ingress. A future C2
execution mutation must separately define the active/usable mapping
requirement. The migration head remains V48; V49 is not required or created.


## C1B REACTIVATE design — designed, unimplemented

REACTIVATE is the administrative inverse of DISABLE and changes ownership
status only. The exact supported transitions are:

```text
EXTERNAL / DISABLED / UNVERIFIED  -> EXTERNAL / ACTIVE / UNVERIFIED
EXTERNAL / DISABLED / VERIFIED    -> EXTERNAL / ACTIVE / VERIFIED
EXTERNAL / DISABLED / INVALIDATED -> EXTERNAL / ACTIVE / INVALIDATED
```

A real transition accepts only a non-native `EXTERNAL` identity in `DISABLED`
ownership state with verification status `UNVERIFIED`, `VERIFIED`, or
`INVALIDATED`. It rejects `LOGOS_NATIVE`, `logos-native`, `REVOKED`, external
`NOT_REQUIRED`, and unsupported state combinations; it does not reinterpret or
repair them. The transition preserves identity ID, namespace, external ID,
target jogador ID, identity class, and verification status. It changes only
`DISABLED` to `ACTIVE` and advances `ownership_version` from N to N+1. An
`INVALIDATED` identity remains invalidated after reactivation. REACTIVATE does
not perform any verification lifecycle operation or define execution
usability; C2 remains separate.

The future `ReactivateExternalSubjectOwnershipCommand` carries the external
subject reference, expected ownership version, and mandatory administrative
reason only. Reason is trimmed, nonblank, and bounded to 512 characters. No
actor, target, state, evidence, or timestamp is command input. Evidence is not
required because reactivation does not prove ownership; history stores
`evidence_type = NULL` and `evidence_reference = NULL`, without changing prior
history.

REACTIVATE reuses exact-namespace `SUBJECT_OWNERSHIP_MANAGE` authority and the
authenticated, verified `WORKLOAD` operator context. Authorization precedes
lookup disclosure. The audit actor is `WORKLOAD_OPERATOR`, derived from the
trusted server-side principal ID, and is never command input. No new
authorization operation is introduced.

For a real transition, `expectedOwnershipVersion` must equal the locked current
version; stale writes conflict. A supported already-active `EXTERNAL` identity
with `UNVERIFIED`, `VERIFIED`, or `INVALIDATED` status is a valid idempotent
replay after reason validation. Stale expected version is ignored and replay
causes no mutation, version increment, history append, reason replacement, or
timestamp change. It is an `ensure ACTIVE` replay and does not imply prior
DISABLED history. Processing order is: authorize namespace, locked lookup,
not-found handling, reason validation, valid replay detection, eligible
DISABLED-state validation, expected-version validation, domain transition,
then atomic persistence.

A real transition appends one immutable `OWNERSHIP_REACTIVATED` event at
aggregate version N+1 with `LOGOS_OPERATOR_ACTION` provenance. The snapshot
records `EXTERNAL` before/after, the unchanged target before/after, `DISABLED`
to `ACTIVE`, and the unchanged eligible verification status. It records the
trusted `WORKLOAD_OPERATOR` actor, NULL evidence fields, mandatory normalized
reason, server-derived `effective_at`, and database-derived `recorded_at`. The
existing identity-row `PESSIMISTIC_WRITE` lock remains the serialization
point. Current state/version and history append must commit atomically; history
failure leaves ownership `DISABLED` and version unchanged. Concurrent equivalent
requests produce one mutation, one increment, one history event, and a valid
replay for the later lock holder.

The physical schema supports the design without a migration. V47 defines
ownership and verification states as `VARCHAR(32)` checks that allow `ACTIVE`,
`DISABLED`, `UNVERIFIED`, `VERIFIED`, and `INVALIDATED`, and excludes
`NOT_REQUIRED` for external identities. History `event_type` is `VARCHAR(64)`
with a nonblank check and no enum or event allowlist;
`OWNERSHIP_REACTIVATED` fits. Append-only triggers reject UPDATE, DELETE, and
TRUNCATE but permit new history inserts. The reason column is `VARCHAR(512)`;
evidence columns are nullable and `recorded_at` has a database default. V48
changes authorization constraints only. Migration head remains V48; V49 is not
required.

REACTIVATE is designed but remains unimplemented and noncanonical until a
separate implementation review and merge. REVOKE remains undesigned; its
terminality, verification preservation, evidence requirements, and replay
semantics are not frozen. TRANSFER, REASSIGNMENT, TARGET_CORRECTION, and C2
remain unimplemented and are not designed by this gate.
