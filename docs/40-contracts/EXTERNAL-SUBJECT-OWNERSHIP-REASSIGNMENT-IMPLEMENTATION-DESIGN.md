# External Subject Ownership REASSIGNMENT — implementation design

Status: implementation architecture only. REASSIGNMENT remains canonical
DESIGN / UNIMPLEMENTED. This document does not create V49, authorize code,
tests, ingress, or a change to the frozen design.

## 1. Binding decisions

The canonical lifecycle decisions remain unchanged:

- `MODEL_B`: create a successor identity for the same external locator.
- Only the current `EXTERNAL / REVOKED` binding can be a predecessor, bound by
  its identity UUID and exact ownership version. ACTIVE and DISABLED are
  ineligible.
- The predecessor remains permanently REVOKED. Its UUID, target, version, and
  history do not change.
- The successor has a new UUID, same namespace/external ID, a distinct target,
  EXTERNAL class, DISABLED ownership, and an independent version stream at 0.
- Successor verification is UNVERIFIED from predecessor UNVERIFIED or VERIFIED;
  INVALIDATED remains INVALIDATED. NOT_REQUIRED is rejected.
- The initial successor history is `OWNERSHIP_REASSIGNED` at aggregate version
  0. Evidence type is fixed as `ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION`.
- Reuse `SUBJECT_OWNERSHIP_MANAGE`, exact namespace, verified WORKLOAD,
  `WORKLOAD_OPERATOR`, and trusted server-derived principal ID.
- V49 is required. No migration or runtime change is authorized here.

### Proposed implementation slicing

Use **R1_R2**. R1 establishes the pointer schema and migrates all identity
selection and locking while preserving the behavior of the seven existing
lifecycle operations. R2 adds the reviewed authorization-record workflow and
the REASSIGNMENT operation. Keeping approval/successor activation out of R1
makes the schema change auditable before the unilateral recovery path exists.

## 2. Read-only implementation inventory

The inventory below is from the V48 canonical main tree. The repositories and
callers currently treat a locator as one identity row.

| Component / call site | Current behavior and required R1 treatment |
| --- | --- |
| `ProgressionSubjectIdentity` entity | Maps UUID, namespace, external ID, required `Jogador`, class, ownership, verification, and version. Add lineage mapping only with V49; preserve existing field meanings. |
| `ProgressionSubjectIdentityJpaRepository.findByNamespaceAndExternalId` | Declared exact-locator `Optional` lookup; no production caller found in source search. Replace/deprecate in favor of explicit current lookup. |
| `findByNamespaceAndExternalIdForUpdate` | Used by lifecycle store; currently locks the identity row selected by locator. Replace with pointer-first `findCurrentForUpdate`. |
| `findAppUserIdByNamespaceAndExternalId` | Used by `JpaExternalSubjectResolver` and the provisioning adapter. Replace with pointer join; do not allow arbitrary historical row selection. |
| `insertIfAbsent` | Used by provisioning and depends on V32 full-locator uniqueness with `ON CONFLICT (namespace, external_id)`. Replace with pointer-key first-binding transaction and guarded current-row comparison. |
| `JpaExternalSubjectResolver.resolve` | Locator-to-AppUser lookup used by `ExternalSubjectProgressionApplicationService`; resolve only through current pointer. |
| `ExternalSubjectProgressionApplicationService.execute` | Calls the resolver before configured progression execution. No C2 decision is made here; only identity selection changes. |
| `JpaSubjectOwnershipLifecycleStore.findForUpdate` | Current locator lookup for lifecycle services. Replace with pointer lock then selected identity lock. |
| Seven lifecycle services: VERIFY, INVALIDATE, REVERIFY, DISABLE, REACTIVATE, REVOKE, TRANSFER | Each authorizes exact namespace before `findForUpdate`; preserve that ordering and all state/replay semantics. All writes must adopt pointer-first locking before identity locking. |
| `JpaSubjectOwnershipLifecycleStore.save*` | Reloads the already-selected identity by UUID and applies version/status/verification/target changes plus history. Keep writes bound to the UUID returned by current-pointer selection; never re-resolve to an arbitrary locator row. |
| `ProgressionSubjectOwnershipHistoryJpaRepository` / `ProgressionSubjectOwnershipHistoryEntry` | History is keyed by identity UUID and aggregate version; transfer replay queries the exact event/version. Add reassignment lineage/correlation columns and a request lookup while retaining UUID-based history. |
| `JpaProgressionSubjectIdentityProvisioningAdapter.provision` | Finds the target player, inserts-if-absent, resolves owner by locator, and appends initial history. Replace with pointer-first provisioning rules below. |
| `ProvisionCurrentExternalSubjectIdentityService` | Provisions non-native external identity for the authenticated user's player. Preserve its current same-target/conflict behavior; it may not create a successor. |
| `ProgressionSubjectIdentityController` | Existing provisioning ingress. It remains initial provisioning only; it cannot create or select reassignment successors. |
| `RegistrationService.register` | Creates a `logos-native` identity through the provisioning port. Preserve native registration behavior and native exclusion. |
| `ProgressionExternalExecutionJpaRepository.findHistory` | Reads durable execution snapshots by stored subject namespace/external ID, not the current identity table. Keep historical execution lookup semantics; do not redirect it through the pointer. |
| Direct SQL in tests | Several PostgreSQL tests query identity/history by locator. Convert current-binding assertions to pointer joins; queries intended to inspect history must explicitly select identity UUID/history rows. |

A source-wide search found no other production `progression_subject_identity`
SQL lookup. Existing UUID lookups inside the lifecycle store are safe only when
the UUID came from the locked current pointer or is explicitly a historical
lookup. The repository API should distinguish those meanings.

## 3. V49 pointer model and exactly-one-current guarantee

### Selected physical representation

Use a `progression_subject_current_binding` locator/pointer relation, not an
`is_current` boolean:

- primary key: `(namespace, external_id)`;
- non-null `current_identity_id`;
- unique `current_identity_id` so one identity cannot be selected for two
  locators;
- identity candidate key: `(id, namespace, external_id)`;
- pointer composite FK `(current_identity_id, namespace, external_id)` to that
  candidate key, `ON UPDATE RESTRICT`, `ON DELETE RESTRICT`;
- identity composite FK `(namespace, external_id)` to the pointer locator key,
  also restrictive and deferrable.

Make the two directions deferrable until transaction commit. The pointer must
match the selected identity's locator; the identity-to-pointer FK ensures each
known identity locator has a pointer row. The non-null pointer target plus
unique locator key gives at most one selected identity. Together, the two
Fks ensure exactly one selected identity for every locator represented by an
identity row. A pointer row cannot dangle or point across locators.

The circular first-binding references are intentional: a first-provisioning
transaction may insert a pointer that names a not-yet-inserted identity, then
insert that identity; both constraints are checked at commit. Successor
creation inserts a new identity referencing the existing locator row, then
updates the pointer to that same-locator identity in the same transaction.
No `ON UPDATE CASCADE` is allowed. Locator columns are immutable after row
creation; use restrictive FKs and an identity-locator update guard so a
transaction cannot rewrite both sides to disguise a target correction.

`jogador_id` remains required and retains its restrictive target FK. Identity
and history rows are not deleted by reassignment. Keep a non-unique
`(namespace, external_id, id)` index for historical inspection and the
composite unique identity key needed by FKs.

### At most one versus exactly one

A pointer PK alone proves **at most one** current identity per locator. It does
not prove that a pointer exists for every historical identity. The reciprocal
deferrable identity-to-pointer FK plus a non-null pointer target and composite
pointer-to-identity FK supplies the **at least one and locator-matching** side
at commit. A database validation query is still required during migration.

Application checks remain fail-closed: if an identity/history row is found for
a locator but no pointer can be found, report a persistence-integrity failure.
Never treat that state as not-found or as a new locator. Do not rely on a
startup check as a substitute for constraints; a startup/readiness check may
report broken coverage operationally.

### Existing locator uniqueness and branching protection

Drop V32's full `UNIQUE(namespace, external_id)` only after pointer backfill
and coverage validation. Replace it with the composite identity key and a
non-unique locator index. Historical duplicates become legal only as distinct
identity lifecycles sharing one pointer row; they are not all current.

Add nullable `predecessor_identity_id` to identity rows. Require:

- FK to identity `(id, namespace, external_id)` using the successor's same
  locator, restrictive delete behavior;
- `predecessor_identity_id <> id`;
- uniqueness on non-null predecessor ID, so a predecessor has at most one
  successor;
- application and deferred database validation that a reassignment
  successor's predecessor was the pointer-selected REVOKED identity and that
  the successor becomes the pointer-selected row in the same commit.

A later current successor may itself be revoked and receive a child, allowing
A → B → C. A predecessor cannot branch A → B and A → C. Acyclicity follows
from the guarded rule that each newly inserted child must name the previously
current predecessor and must become current; additionally validate chains
with a recursive integrity query in migration/tests. Do not provide an API to
move a pointer back to a historical UUID.

Constraints enforce locator match, pointer uniqueness, FKs, and no branching.
A deferred constraint trigger should reject identity insertion without a
corresponding first-binding pointer or valid reassignment event/pointer switch.
The application still validates the reviewed authorization and all domain
semantics; database constraints are not a replacement for operator
authorization.

## 4. Request correlation and trusted evidence

### Existing mechanism assessment

`EXISTING_TRUSTED_MECHANISM_REUSABLE = NO`.
V45 `workload_principal`, signing-key, replay, and
`workload_trust_audit_event` tables establish workload trust and audit
credential administration. They do not hold reviewed reassignment cases or
bind a review to locator, predecessor version/target, and successor target.
V46/V48 authorization grants establish operation/namespace permission, not a
case approval. Neither is a substitute for reassignment evidence.

### Required reviewed authorization record

Add a dedicated durable authorization record as part of the V49 structural
model; no row is seeded by migration. Conceptual fields:

- immutable `authorization_id UUID` (the command's `evidenceReference` is this
  UUID's canonical string form);
- unique `reassignment_request_id UUID`;
- exact namespace and external ID;
- predecessor identity UUID, expected version, and predecessor target UUID;
- proposed successor target UUID;
- reviewed recovery basis and external case/reference;
- reviewer workload principal ID and DB-derived `reviewed_at`.

Store only completed approvals: row existence means approved; do not create a
mutable PENDING row and later flip it. Protect it from UPDATE/DELETE. Its
insert path must be a future explicit approval use case, after the reviewer
has reviewed the bound case. Require a verified WORKLOAD reviewer authorized
by the existing `SUBJECT_OWNERSHIP_MANAGE` operation for the exact namespace.
The reviewer must differ from the eventual REASSIGNMENT executor. Persist the
reviewer's trusted server-derived principal ID; do not accept actor identity
from input. This is a two-person control using the existing authorization
operation, not a new grant or operation.

`evidenceReference` and `reassignmentRequestId` are distinct. The former
identifies the approved evidence record; the latter identifies one idempotent
attempt and is included in that record. A nonblank arbitrary string is never
approval. R2 must parse the reference as the canonical authorization UUID,
load the immutable record, and compare every bound field exactly. If the
approved-record creation path cannot establish authenticated reviewer
approval under the existing WORKLOAD/namespace authority, stop before enabling
REASSIGNMENT; do not fall back to caller text or a manually trusted flag.

The authorization row binds the exact request, locator, predecessor UUID /
version / target, new target, and recovery basis. It cannot authorize a
changed payload. A one-use authorization is enforced by unique
`reassignment_authorization_id` on the reassignment history event; successful
history is the immutable consumption record. No separate mutable consumed bit
is needed.

### Request ID and replay storage

Use one UUID request ID across the approved authorization record and the
successor's `OWNERSHIP_REASSIGNED` history event. Enforce uniqueness in both
places; the history uniqueness is event-scoped so existing event history is
unaffected. The successor identity UUID is already `history.identity_id`; do
not duplicate it in the event.

After namespace authorization, replay lookup is by `reassignment_request_id`,
not by current pointer or current target. Load the event and its successor by
UUID, then compare the immutable authorization and event to the command:
request ID, authorization ID/reference, predecessor UUID/version/locator/
status/target, successor UUID/target/state/verification, evidence, normalized
reason, actor/provenance as recorded, and correlation. Exact match returns the
original successor. Same request ID with any payload difference conflicts.
A completed replay remains findable even after a later successor becomes
current. A stale predecessor without an exact completed request cannot act on
a newer binding.

## 5. History extension

V47 history remains append-only and retains its unique
`(identity_id, aggregate_version)` key. Existing events must remain valid, so
new fields are nullable for legacy rows:

- `predecessor_identity_id UUID`;
- `predecessor_ownership_version BIGINT`;
- `reassignment_request_id UUID`;
- `reassignment_authorization_id UUID`.

Add restrictive FKs for predecessor identity (including same-locator
integrity where possible) and authorization ID. For `OWNERSHIP_REASSIGNED`,
require all four fields, `identity_id` as the successor, predecessor UUID
other than successor, predecessor version nonnegative, fixed evidence type,
non-null reference and reason, and the frozen before/after snapshot:
EXTERNAL/REVOKED → EXTERNAL/DISABLED with target A → B and the canonical
verification mapping. Require request ID and authorization ID to be unique
for reassignment events. Other event types retain null lineage/correlation
fields. Add an event-specific CHECK if it can express these invariants without
invalidating existing rows; use a trigger for cross-row checks that CHECK
cannot express.

`aggregate_version=0` is valid in V47. A successor receives one initial
`OWNERSHIP_REASSIGNED` row at version 0 instead of an unrelated
`INITIAL_CLASSIFICATION` row. Its `recorded_at` remains DB-derived, and
`effective_at` remains server-derived. Existing event evidence/reason limits
(64/255/512) fit the fixed type, authorization UUID string, and reason. The
successor row's `predecessor_identity_id` and event's predecessor/version
fields link lineage and replay without rewriting predecessor history.

## 6. Repository APIs and lookup migration

Replace ambiguous locator APIs with explicit meanings:

- `findCurrentByReference(reference)` — pointer join, read-only;
- `findCurrentForUpdate(reference)` — lock pointer, then selected identity;
- `findByIdentityId(identityId)` — identity-addressed lookup, documented as
  historical-capable and not implicitly current;
- `findReassignmentByRequestId(requestId)` — immutable event/auth lookup used
  only after exact-namespace authorization.

Do not retain a locator `Optional<Identity>` method whose behavior silently
changes from unique-row lookup to arbitrary historical selection. The
lifecycle store returns a selected current identity plus enough pointer
identity/version context to ensure it has not changed. Historical ownership
history remains queried by identity UUID.

Read resolution is one SQL statement joining locator pointer to the selected
identity and target. A statement observes either the pre-commit or post-commit
pointer/successor state, never a partial reassignment. Execution snapshot
history remains its own stored subject locator and is not rewritten.

## 7. Pointer-first lock protocol

All lifecycle writes use one order:

1. validate/reference namespace needed for authorization;
2. authorize `SUBJECT_OWNERSHIP_MANAGE` for that exact namespace;
3. obtain trusted verified WORKLOAD operator context;
4. lock the locator pointer row `FOR UPDATE`;
5. load/lock the exact `current_identity_id` row `FOR UPDATE` and confirm its
   locator equals the pointer;
6. perform replay, eligibility, version, and target checks in the existing
   operation-specific order;
7. write current identity/history, or for R2 create successor/history and
   switch pointer; commit.

Preserve each existing service's external ordering, especially authorization
before identity disclosure and existing replay-before-stale-version behavior.
Add pointer locking underneath every lifecycle store operation:

| Operation | Required R1 lock change |
| --- | --- |
| VERIFY | Lock pointer before ACTIVE identity row; preserve VERIFY/no-op/version rules. |
| INVALIDATE | Same; preserve invalidation evidence and replay rules. |
| REVERIFY | Same; preserve event-backed replay and eligibility. |
| DISABLE | Same; preserve supported replay and status semantics. |
| REACTIVATE | Same; continue rejecting REVOKED. |
| REVOKE | Same; retain terminal current-binding semantics. |
| TRANSFER | Same; preserve bilateral evidence, exact replay, target change, and version semantics. |
| REASSIGNMENT (R2) | Same order; only after committed REVOKE, then create successor and switch pointer. |

Provisioning locks/claims the pointer key first; see §9. Resolver reads do
not lock. No target-row lock is required for existence: the target FK is the
final integrity guard. If an implementation chooses to lock a target row, it
must do so only after pointer and identity locks and use one stable target
ordering across write paths. Prefer not to add a target lock. Avoid reverse
identity→pointer locking anywhere; it can deadlock with reassignment.

## 8. R1 behavior-preserving infrastructure slice

### Scope

R1 includes V49, pointer/lineage/correlation structures, repository API
replacement, pointer-first locking for all current lifecycle writes, current
resolver selection, provisioning changes, and structural tests. R1 contains
no REASSIGNMENT command/use case, no successor creation path, no authorization
record approval writer, and no `OWNERSHIP_REASSIGNED` production event.
The authorization-record table may exist structurally but remains unused.

### Migration ordering

V49 must run as a validated transaction before any application instance uses
the new model:

1. Preflight duplicate-locator, missing-target, identity-class/status,
   history-FK, and history-version integrity. V48's full unique locator means
   the initial dataset has at most one identity per locator.
2. Add the identity candidate key `(id, namespace, external_id)` and a
   non-unique historical locator index.
3. Create the pointer relation and its deferred composite FK to identity.
4. Backfill exactly one pointer for every existing identity/locator, selecting
   that sole V48 identity as current.
5. Validate counts and joins: every identity locator has exactly one pointer;
   every pointer resolves to the same identity locator; no pointer is
   duplicated or orphaned.
6. Add the deferred identity-to-pointer locator FK and make pointer target
   non-null/unique, establishing exactly-one-current at commit.
7. Add predecessor, authorization-record, history-correlation fields,
   restrictive FKs, unique indexes, event checks, and integrity triggers.
   Existing events receive NULL in new history columns.
8. Re-run coverage, FK, status, history version, and acyclic-lineage
   validations.
9. Only after all validation succeeds, drop V32's full locator unique
   constraint. Keep the non-unique locator index and the new exactly-one
   pointer invariant.

On a migration failure, the migration must abort and leave V48 usable; do not
ignore validation failures or partially drop uniqueness. V49 should be
transactional where PostgreSQL/Flyway permits. Once reassignment-created
historical duplicates exist, a down migration cannot safely restore full
locator uniqueness; recovery is forward-only. R1 must document rollback and
forward-repair operations before rollout.

### Exactly-one validation and creation rules

The reciprocal FKs enforce coverage for committed identity rows after V49.
Migration validation proves the initial backfill. Application integrity
checks convert any unexpected missing/mismatched pointer into a persistence
integrity error. A fresh first-binding transaction inserts the pointer claim
and identity row together under deferred FKs. A unique pointer-key conflict
serializes concurrent first claims; the loser re-reads current in a new valid
transaction and applies existing same-target idempotency/different-target
conflict behavior. It must not insert a second identity first and hope a
locator constraint catches it.

A deferred guard validates that newly inserted first identities become their
locator's pointer, and that future successor rows are introduced only with a
valid predecessor/reassignment event and pointer switch. The unique
predecessor link blocks branching at the database. The authorization service
remains responsible for verifying the trusted approval; the database only
checks its bound fields and referential consistency.

## 9. Provisioning and resolver behavior

| Case | Required R1 behavior |
| --- | --- |
| A. No identity history and no pointer | May create exactly one initial identity and pointer atomically, with its existing initial history. |
| B. Current pointer exists | Apply current provisioning behavior against that selected row. Same current target remains idempotent; a different target conflicts. Never change ownership/verification or pointer. |
| C. Historical identity exists but pointer missing | Persistence-integrity error. Never treat as first provisioning. Constraints should make this unreachable after V49. |
| D. Current binding is REVOKED | Provisioning cannot create a successor or change the pointer. Same-target confirmation may preserve the existing response only if it has no mutation; different target conflicts. REASSIGNMENT is the only successor path. |
| E. Current binding is DISABLED | No replacement and no reactivation. Preserve existing same-target response; different target conflicts. |
| F. Current binding is ACTIVE | Keep it current; preserve existing same-target response; different target conflicts. |

The provisioning adapter's current `ON CONFLICT(namespace,external_id)` must
be removed because that full unique constraint goes away. Claim/lock the
pointer key, then load its current identity. For an initial claim, use one
transaction and deferred FK semantics. Registration still provisions native
identity only; native is excluded from reassignment. If a unique-key race
resolves to another first provision, rollback and re-read the selected pointer
before applying current same-target/conflict behavior.

Resolver becomes locator → pointer → identity → target. It must never return a
historical predecessor. A missing pointer for a known locator is an integrity
failure, not ordinary not-found. This selection change is part of identity
architecture; it does not change C2 execution policy. `ProgressionExternalExecutionJpaRepository.findHistory` continues to read
stored execution snapshots and does not use the current pointer.

## 10. R2 REASSIGNMENT slice and entry gate

R2 begins only after R1 is merged, V49 is canonical, all migration/coverage
checks pass, and all seven existing lifecycle operations and provisioning use
the pointer-first architecture without semantic changes.

R2 includes:

1. A separate approval-record use case, authenticated/verified WORKLOAD,
   exact namespace authorization, and reviewer identity from trusted context.
   Approval record creation is only after review; reviewer differs from the
   later executor. No new authorization operation, grant, principal, or trust
   seed is added.
2. The frozen `ReassignExternalSubjectOwnershipCommand` fields. Resolve
   `evidenceReference` as the canonical UUID of the immutable approved record;
   compare the request UUID and every locator/predecessor/version/target claim.
3. Authorize before approval, request-history, pointer, or identity lookup.
   Lock pointer then current predecessor. A completed exact replay is resolved
   by request UUID after authorization and may return an older original
   successor even if a later lifecycle now owns the pointer.
4. For a real operation, require pointer-selected predecessor UUID, exact
   version N, EXTERNAL/REVOKED, reviewed approval matching payload, and a
   distinct existing target. Reject ACTIVE/DISABLED/native/NOT_REQUIRED or
   stale predecessor. Create new UUID with EXTERNAL/DISABLED, mapped
   verification, and version 0; append `OWNERSHIP_REASSIGNED` at version 0;
   switch pointer in the same transaction.
5. Atomically consume the authorization via the unique event authorization
   ID. Event/history and pointer become the immutable completion record.
   Failure rolls back successor, history, and pointer; predecessor is
   untouched.

R2 entry is blocked if the reviewed authorization record cannot be populated
only by a trusted authenticated approval path. A nonblank reference, external
case number, or user-supplied reviewer string alone never satisfies evidence.

## 11. Deployment compatibility and R1 exit criteria

`OLD_APP_NEW_SCHEMA_COMPATIBLE = NO`: old exact-locator `Optional` methods
can return multiple rows after uniqueness is relaxed; old provisioning names
the removed full unique constraint in `ON CONFLICT`.
`NEW_APP_OLD_SCHEMA_COMPATIBLE = NO`: pointer tables/FKs do not exist, and new
queries cannot safely select current identity. Therefore
`ATOMIC_DEPLOYMENT_REQUIRED = YES` for schema plus pointer-aware application.
Quiesce old writers/resolvers, migrate and deploy the compatible application
as one coordinated release, then start the new fleet. Do not run mixed old/new
instances. A staged expand/contract rollout needs a separate compatibility
design.

R1 is complete only after:

- V49 upgrades a representative V48 database and boots a fresh database;
- backfill validates every existing identity exactly once and every pointer
  matches identity locator; no historical duplicates are introduced in R1;
- pointer constraints reject mismatched locator, dangling identity, missing
  pointer, duplicate current, lineage branch, self-link, or invalid chain;
- every repository lookup has explicit current or historical semantics;
- resolver and provisioning select/guard the current pointer;
- all seven existing lifecycle operations pass their unchanged semantic
  regression tests, including replay, event, rollback, and PostgreSQL
  concurrency coverage; TRANSFER remains exactly canonical;
- all lifecycle writes and provisioning follow pointer-first lock order and
  lock/concurrency tests show no stale-row mutation or deadlock;
- registration's native mapping and target/history FKs remain intact;
- V49 migration failure leaves V48 intact, and fresh V49 bootstrap succeeds;
- no REASSIGNMENT command, approval writer, successor creation path, ingress,
  or `OWNERSHIP_REASSIGNED` event can be invoked;
- full PostgreSQL suite and `git diff --check` pass; C2 behavior is unchanged.

## 12. Unchanged boundaries

This architecture does not design TARGET_CORRECTION, change any canonical
lifecycle transition, modify historical execution snapshots, or implement C2.
An ACTIVE binding does not thereby become execution-authorized. V49 is a future
migration requirement, not an authorization to create it now.
