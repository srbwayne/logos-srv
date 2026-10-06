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
implemented C1B lifecycle mutations are VERIFY, INVALIDATE, REVERIFY, DISABLE,
REACTIVATE, and REVOKE. TRANSFER is designed but unimplemented. REASSIGNMENT,
TARGET_CORRECTION, and C2 enforcement remain unimplemented and undesigned.

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
progression execution behavior. REACTIVATE and REVOKE are canonical and
implemented. TRANSFER is designed below but unimplemented; REASSIGNMENT,
TARGET_CORRECTION, and C2 remain undesigned and unimplemented.

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
REACTIVATE and REVOKE are canonical and implemented. TRANSFER is designed but
unimplemented. REASSIGNMENT, TARGET_CORRECTION, and C2 enforcement remain
unimplemented and undesigned.

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

REACTIVATE became canonical when PR #63 was squash-merged as
`2b8a5ac1aaf4e5639612090bcb57a5892168fc02`. REVOKE is canonical and
implemented. TRANSFER is designed below but remains unimplemented.
REASSIGNMENT, TARGET_CORRECTION, and C2 remain unimplemented and undesigned.

## C1B REVOKE — canonical implementation semantics

The implementation became canonical in PR #65. This section records the
frozen transition semantics and schema basis that were designed before that
merge.

REVOKE terminates the current external ownership binding. It is stronger than
DISABLE and terminal for this binding lifecycle; REACTIVATE does not accept a
REVOKED source. This does not decide whether a separately authorized future
operation may create a new binding, transfer ownership, reassign ownership, or
correct a target. No unrevoke, recovery, or automatic rebinding is defined.

The only real REVOKE transitions are:

```text
EXTERNAL / ACTIVE   / UNVERIFIED  -> EXTERNAL / REVOKED / UNVERIFIED
EXTERNAL / ACTIVE   / VERIFIED    -> EXTERNAL / REVOKED / VERIFIED
EXTERNAL / ACTIVE   / INVALIDATED -> EXTERNAL / REVOKED / INVALIDATED
EXTERNAL / DISABLED / UNVERIFIED  -> EXTERNAL / REVOKED / UNVERIFIED
EXTERNAL / DISABLED / VERIFIED    -> EXTERNAL / REVOKED / VERIFIED
EXTERNAL / DISABLED / INVALIDATED -> EXTERNAL / REVOKED / INVALIDATED
```

Eligibility is limited to an `EXTERNAL` identity outside `logos-native` with
ownership `ACTIVE` or `DISABLED` and verification `UNVERIFIED`, `VERIFIED`, or
`INVALIDATED`. Native identities, the native namespace, `NOT_REQUIRED`,
unsupported states, and `REVOKED` as a real-transition source are rejected.
A real transition changes only ownership status to `REVOKED` and advances
`ownership_version` from N to N+1. It preserves identity ID, namespace,
external ID, target jogador ID, identity class, and verification status
exactly; it does not verify, invalidate, or reverify. REVOKE is a state
transition, never hard deletion, and preserves the current identity row and
all prior history.

The future `RevokeExternalSubjectOwnershipCommand` contains only
`ExternalSubjectReference reference`, `long expectedOwnershipVersion`, and a
mandatory administrative reason. The reason is trimmed, nonblank, and at most
512 characters; reuse `SubjectOwnershipAdministrativeReason`. No actor,
target, status, evidence, or timestamp is command input. Evidence is not
required: `evidence_type` and `evidence_reference` are NULL. Audit uses
`LOGOS_OPERATOR_ACTION`, actor type `WORKLOAD_OPERATOR`, and the trusted
server-derived principal ID.

Authorization reuses exact-namespace `SUBJECT_OWNERSHIP_MANAGE` and occurs
before identity lookup disclosure. The operator remains an authenticated,
verified `WORKLOAD`. Processing order is: authorize namespace; lock and look up
the identity; report not-found only after authorization; validate the reason;
detect a supported already-REVOKED replay; validate eligible ACTIVE or
DISABLED source state; validate expected version; apply the domain transition;
and atomically persist current state/version and history. A real transition
requires the expected version to match the locked version; a stale version
conflicts.

A supported `EXTERNAL / REVOKED` state with verification `UNVERIFIED`,
`VERIFIED`, or `INVALIDATED` is a valid ensure-revoked replay after reason
validation. Replay ignores a stale expected version and succeeds without
mutation, version increment, history append, reason replacement, or timestamp
replacement. It does not rewrite or replace the original revocation reason.
Unsupported revoked shapes are not replay-valid.

A real transition appends exactly one immutable `OWNERSHIP_REVOKED` event at
aggregate version N+1 with `LOGOS_OPERATOR_ACTION` provenance. The snapshot
records `EXTERNAL` before and after; the same target before and after;
`ACTIVE` or `DISABLED` to `REVOKED`; and the same eligible verification state
before and after. It includes the trusted `WORKLOAD_OPERATOR` actor, NULL
evidence fields, normalized reason, server-derived `effective_at`, and
database-derived `recorded_at`.

REVOKE reuses the identity-row `PESSIMISTIC_WRITE` lock. State/version mutation
and the history append must commit in one transaction. If history persistence
fails, ownership remains in its source state and the version remains N. Two
equivalent concurrent requests serialize to one transition, one version
increment, and one `OWNERSHIP_REVOKED` event; the later lock holder succeeds as
a valid replay.

The physical schema supports the design without a migration. V47 allows
`REVOKED` in the ownership-status check and excludes `NOT_REQUIRED` for
external identities. `event_type` is nonblank `VARCHAR(64)` without an enum or
event-name allowlist; `OWNERSHIP_REVOKED` fits. The reason is `VARCHAR(512)`,
evidence columns are nullable, and `recorded_at` is database-defaulted.
Append-only triggers reject UPDATE, DELETE, and TRUNCATE while permitting
history INSERT. V48 changes authorization constraints only. The migration
head remains V48; V49 is not required.

REVOKED is terminal only for the current binding. TRANSFER is separately
designed below for eligible ACTIVE or DISABLED bindings and cannot revive a
REVOKED binding or define replacement bindings. REASSIGNMENT remains
undesigned. This design does not define resolver or progression-execution
enforcement: C2 must separately decide how ownership status affects execution
eligibility and remains unimplemented. REVOKE is canonical and implemented;
TRANSFER is designed but unimplemented. TARGET_CORRECTION and C2 remain
undesigned.


## C1B TRANSFER design — proposed, unimplemented

This proposed design is documentation-only. It does not alter the canonical
VERIFY, INVALIDATE, REVERIFY, DISABLE, REACTIVATE, or REVOKE semantics and
confers no implementation authority.

### Frozen behavior and physical model

The locator `(namespace, externalId)` is not ownership proof. Bindings remain
immutable by default and history append-only; `LOGOS_NATIVE` and
`logos-native` remain Logos-controlled. REVOKED is terminal for the current
binding. C2 is independent and unimplemented.

Inspection of V32/V47/V48 and the current model confirms that
`progression_subject_identity` has a UUID identity, a required `jogador_id`
foreign key, and full uniqueness on `(namespace, external_id)`. Exact-locator
repository lookup returns one optional row; lifecycle mutation uses
`PESSIMISTIC_WRITE` on that row. No current-binding marker or predecessor /
successor link exists. V47 history is keyed by identity and aggregate version,
records before/after targets and lifecycle states, permits arbitrary nonblank
`VARCHAR(64)` event names, nullable evidence (`VARCHAR(64)` type and
`VARCHAR(255)` reference), nullable `VARCHAR(512)` reason, and database-defaulted
`recorded_at`. Its triggers prohibit UPDATE, DELETE, and TRUNCATE but allow
INSERT. Target and identity foreign keys are restrictive. V48 changes
authorization constraints only.

### Model decision: same-row target change

Select Model A: keep the same current identity row and UUID, locator, class,
and ownership status, while changing target jogador A to distinct target B
through this explicit evidence-backed operation. Increment its existing
ownership version once and append complete target/state snapshots. This is a
narrow audited exception to immutability by default, not a generic target
mutation.

Reject Model B (terminate old row and create a successor row): the full
locator unique constraint prevents coexistence, lookup has no current-row
selection rule, and the schema has no cross-row lineage or transfer
correlation. That model requires schema support for historical rows and a
unique current row, predecessor/successor linkage, and matching lookup/resolver
changes. No Model C is naturally represented by the inspected code/schema.
Model A fits the mutable target column and existing history fields; no schema
change is required, so `V49_REQUIRED = NO`. This is only a compatibility
finding.

### Meaning and allowed states

TRANSFER is an intentional legitimate handoff of the same external locator
from A to distinct B, evidenced as a release by A and acceptance by B. It is
allowed from EXTERNAL ACTIVE and EXTERNAL DISABLED, preserving the ownership
status. DISABLED remains disabled. It is forbidden from REVOKED, which remains
terminal for the current binding. Native identities, `logos-native`, and
EXTERNAL / NOT_REQUIRED are excluded.

Verification is a property of the external-subject-to-target binding, not the
locator alone. Therefore:

| Verification before | Verification after target handoff | Rationale |
| --- | --- | --- |
| UNVERIFIED | UNVERIFIED | No verified assertion exists. |
| VERIFIED | UNVERIFIED | Proof connecting the subject to A does not establish the connection to B. |
| INVALIDATED | INVALIDATED | Preserve the negative signal; changing target must not launder it. |
| NOT_REQUIRED | rejected | Invalid for EXTERNAL under V47. |

TRANSFER performs no VERIFY, INVALIDATE, or REVERIFY. Its evidence is not
verification proof. A later separately authorized verification action is
needed to reach VERIFIED; only REVERIFY can change INVALIDATED under its
existing rules.

### Command, authorization, reason, and evidence

The proposed `TransferExternalSubjectOwnershipCommand` contains the external
subject reference, expected ownership version, new target jogador UUID,
transfer evidence type/reference, and mandatory administrative reason. It
contains no actor, lifecycle status, verification status, or timestamps. No
separate idempotency key is proposed.

Reuse exact-namespace `SUBJECT_OWNERSHIP_MANAGE`. Authorization precedes
lookup/disclosure, then the locked row is validated. Require an authenticated
verified WORKLOAD. History actor remains `WORKLOAD_OPERATOR` with trusted
server-derived principal ID. No new authorization operation or actor input is
introduced.

Reason is trimmed, nonblank, and at most 512 characters. Require separate
transfer evidence, not `OwnershipVerificationEvidence`: the proposed fixed
type is `BILATERAL_TRANSFER_CONSENT`, with trimmed nonblank opaque reference
of at most 255 characters. The referenced record must identify the exact
locator and old/new target IDs, attest release by A and acceptance by B, and
record operator review. This evidence supports the administrative handoff; it
does not prove external identity or change verification status.

### Version, replay, history, and atomicity

A real transfer requires expected version N and changes it to N+1. It
preserves identity UUID, locator and EXTERNAL class; target changes A to B;
ownership status is unchanged; verification follows the table above. Reject
same-target requests.

Replay is not inferred from current target alone. Accept an equivalent retry
only if current version equals expected version plus one and the immutable
history event at that version proves the same identity/locator, old target A,
new target B, before/after ownership and verification states, evidence type
and reference, and normalized reason under `OWNERSHIP_TRANSFERRED`. Otherwise
stale or competing requests conflict. Valid replay has no mutation, version
increment, event, or timestamp change.

Append one immutable `OWNERSHIP_TRANSFERRED` event with full before/after
class, target, ownership, verification, and version snapshots;
`LOGOS_OPERATOR_ACTION`; trusted `WORKLOAD_OPERATOR`; evidence; normalized
reason; server-derived `effective_at`; and database-derived `recorded_at`.
The existing identity-row `PESSIMISTIC_WRITE` lock serializes target/version
change and history append in one transaction. History failure rolls back both.

### Concurrency and boundaries

Equivalent concurrent A-to-B requests yield one transition/event; the later
lock holder succeeds only through the exact history-backed replay test.
Competing A-to-B and A-to-C requests cannot both succeed: the winner commits
and the other conflicts. TRANSFER racing DISABLE, REACTIVATE, REVOKE, VERIFY,
INVALIDATE, or REVERIFY serializes on the same row and version; the stale
mutation conflicts. A transfer retried explicitly after another operation
may proceed only if the then-current state is ACTIVE or DISABLED. REVOKE winning
makes transfer ineligible; transfer winning first lets a correctly versioned
later REVOKE act on the new target. A DISABLED transfer racing REACTIVATE
follows the same lock/version rule. Verification races do not imply or reorder
proof; no automatic retries are allowed.

TRANSFER is a consensual handoff. REASSIGNMENT remains a separate, undesigned
recovery/replacement flow (for example, an unavailable source target or a
separately authorized binding after revocation); TRANSFER cannot bypass
REVOKE. TARGET_CORRECTION is a clerical fix to erroneous target data, not a
handoff; it needs distinct evidence and immutable audit history and must not
bypass transfer auditing. Its semantics remain unfrozen. Neither is
implemented. No resolver, ingress, or execution behavior changes; ACTIVE does
not mean executable and C2 remains separate.
