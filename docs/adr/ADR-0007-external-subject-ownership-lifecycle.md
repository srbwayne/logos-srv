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
VERIFY, INVALIDATE, and REVERIFY capabilities are the implemented C1B lifecycle
mutations. DISABLE, REACTIVATE, REVOKE, TRANSFER, REASSIGNMENT,
TARGET_CORRECTION, and C2 enforcement remain unimplemented.

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
progression execution behavior. Disable, reactivate, revoke, transfer,
reassignment, target correction, and C2 enforcement remain unimplemented.

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
DISABLE, REACTIVATE, REVOKE, TRANSFER, REASSIGNMENT, TARGET_CORRECTION, and C2
enforcement remain unimplemented.
