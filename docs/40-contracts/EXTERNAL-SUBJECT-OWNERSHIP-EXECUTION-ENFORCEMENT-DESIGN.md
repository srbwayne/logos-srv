# External Subject Ownership Execution Enforcement (C2)

## Status and scope

Status: canonical design, implementation not authorized by this document.
This contract freezes C2 eligibility, idempotency, ordering, concurrency,
errors, migration, and rollout boundaries for new external progression
mutations. It does not implement runtime enforcement, activate ingress, change
grants or trust, or change C1B lifecycle behavior.

```text
C2_DESIGN = CANONICAL
C2_RUNTIME = NOT_IMPLEMENTED
C2_MIGRATION_REQUIRED = NO
```

## Security boundary

`(namespace, externalId)` is a locator, not ownership proof. HARD-001 proves
workload identity. HARD-002 proves permission for an operation in a namespace.
Neither proves ownership of an external subject. A new external progression
mutation requires all three: authenticated workload, exact
`PROGRESSION_EXECUTE` authorization, and an eligible current ownership
binding.

The enforced order is:

```text
HARD-001 authentication
→ HARD-002 exact PROGRESSION_EXECUTE authorization
→ C2 current-binding eligibility
→ progression mutation
```

HARD-002 must run before ownership lookup or disclosure. C2 adds no authorization
operation or grant, does not require `SUBJECT_OWNERSHIP_MANAGE` for progression,
and never derives ownership from a grant. Lifecycle administration remains a
separate capability.

## External eligibility policy

For a **new** external progression mutation, the identity selected by the
current-binding pointer is executable iff all conditions hold:

```text
identity_class = EXTERNAL
ownership_status = ACTIVE
verification_status = VERIFIED
```

This is policy V2. ACTIVE is lifecycle status, not ownership proof.
UNVERIFIED means positive ownership proof has not been recorded. INVALIDATED
is a negative signal. Neither is usable for a new mutation.

| Current identity classification | New external mutation |
| --- | --- |
| EXTERNAL / ACTIVE / UNVERIFIED | Deny |
| EXTERNAL / ACTIVE / VERIFIED | Allow |
| EXTERNAL / ACTIVE / INVALIDATED | Deny |
| EXTERNAL / DISABLED / any verification state | Deny |
| EXTERNAL / REVOKED / any verification state | Deny |

REACTIVATE changes DISABLED to ACTIVE but preserves verification; it does not
make UNVERIFIED or INVALIDATED executable. VERIFY or REVERIFY must commit
ACTIVE/VERIFIED before a new external mutation can proceed. REVOKE remains
terminal for that identity. TRANSFER evaluates the pointer-selected binding
after the transfer; its verification mapping remains governed by the canonical
TRANSFER contract. REASSIGNMENT and TARGET_CORRECTION successors start
DISABLED, so neither becomes executable implicitly.

### Native identities

`LOGOS_NATIVE / ACTIVE / NOT_REQUIRED` is outside external ownership
verification enforcement and remains operational through the native
progression path. C2 must not require VERIFIED for native identities. Where a
shared execution path receives a native mapping, it must preserve the native
classification and existing native behavior rather than treat NOT_REQUIRED
as an invalid external verification value. HARD-002 requirements for a
workload request remain independent.

## Currentness and enforcement boundary

`progression_subject_current_binding` is the only source of currentness. C2
must evaluate the pointer-selected identity; it must not scan for an ACTIVE
row, choose a historical identity, or infer currentness from ownership status.
A historical predecessor may remain ACTIVE after successor creation. If
identity/history exists but its current pointer is absent or structurally
invalid, report an integrity failure; do not fall back to subject-not-found or
historical resolution.

Use a dedicated execution-eligibility application capability, conceptually
`ExternalSubjectExecutionEligibility`, backed by a narrow persistence adapter
that selects and locks the pointer, locks the selected identity, and returns
the snapshot needed for eligibility. Keep `ExternalSubjectResolver` as an
identity-resolution abstraction. Do not embed general execution policy into a
resolver that may later serve reads or other consumers.

## Idempotency and replay

An exact previously **COMPLETED** execution is a replay, not a new mutation.
After matching execution identity and exact request fingerprint, return its
durable immutable outcome without consulting current ownership. This remains
true after DISABLE, REVOKE, TRANSFER, REASSIGNMENT, TARGET_CORRECTION, or
verification changes. A changed fingerprint remains an idempotency conflict.
Current ownership controls new mutations only; it never reinterprets a
completed result.

The execution table is also used by activity/recovery flows, so row existence
alone is not a completed replay:

| Persisted processing status | External execution behavior |
| --- | --- |
| `COMPLETED`, matching fingerprint | Return the immutable durable outcome. |
| `COMPLETED`, different fingerprint | Return bounded idempotency conflict. |
| `PENDING` | Not a completed replay; fail closed with bounded 409. |
| `PROCESSING` | Not a completed replay; fail closed with bounded 409. |
| `FAILED` | Not a completed replay; fail closed with bounded 409. |

Do not deserialize placeholder response bodies, including `PENDING`, as a
`ProgressionOutcome`. Activity/recovery retry behavior remains owned by its
existing path and is not changed by C2.

Normal external execution reserves the idempotency row, mutates progression,
and persists the completed outcome in one transaction. A failure that rolls
back this transaction leaves no committed reservation and may be retried
normally. A persisted FAILED row is not that rollback state and must not be
automatically reused by external execution; it fails closed with 409 and no
progression mutation.

### Two-stage idempotency check

A fast lookup may return an exact completed replay before starting the
transaction. Any request not resolved there follows this transactional order:

```text
initial completed-replay lookup
→ transaction
→ configuration resolution as appropriate
→ lock current-binding pointer
→ lock pointer-selected identity
→ second execution identity/status check
→ return COMPLETED replay or fail non-COMPLETED/conflicting identity
→ evaluate C2 eligibility
→ reserve new execution identity
→ lock progression profile/player
→ mutate progression
→ persist durable snapshot
→ commit
```

The second identity/status check after ownership locks closes the wait/race
window before reservation and mutation. Configuration-resolution placement may
be refined without weakening HARD-002 ordering, replay semantics, or ownership
serialization. A unique database constraint remains necessary for concurrent
same-key requests.

## Ownership concurrency and lock order

Execution and C1B lifecycle writes serialize on the same PostgreSQL ownership
rows. For new execution, acquire and retain locks through outcome commit in
this order:

```text
current-binding pointer
→ selected identity
→ idempotency status/reservation
→ progression profile/player
→ mutation
→ durable snapshot
→ commit
```

Execution must use the same pointer-then-identity order as pointer-changing
C1B operations. Never lock identity before pointer. Ownership version checks
alone are insufficient: pointer movement can make an unchanged ACTIVE row
historical. The pointer and identity locks remain held until mutation and
durable snapshot commit.

The first transaction that obtains the relevant ownership locks and commits a
valid state wins; an aborted transaction makes no state transition and the
waiting transaction reevaluates committed state.

| Race | Required result |
| --- | --- |
| Execution vs DISABLE | DISABLE first denies execution. Execution first commits fully before DISABLE proceeds. |
| Execution vs REVOKE | REVOKE first denies execution. Execution first may commit before terminal revocation. |
| Execution vs TRANSFER | Transfer first makes execution observe the committed current target/state. Execution first completes against the prior target before transfer proceeds. |
| Execution vs REASSIGNMENT | Pointer transition serializes; the new DISABLED successor is non-executable. |
| Execution vs TARGET_CORRECTION | Pointer transition serializes; the new DISABLED successor is non-executable. |
| Execution vs REACTIVATE | Execution reevaluates committed ACTIVE state and still requires VERIFIED. |
| Execution vs VERIFY / REVERIFY | After ACTIVE/VERIFIED commits, new execution may proceed. |
| Execution vs INVALIDATE | After invalidation commits, new execution is denied. |

This design requires execution to join the existing lifecycle serialization
point. It does not change C1B lifecycle semantics or introduce generic pointer
movement. PostgreSQL concurrency tests must prove lock order, winner behavior,
absence of partial mutation, and absence of deadlock.

## Error contract

Known locator with an ineligible current external binding returns one coarse
`403 Forbidden` application/security-facing error. Do not reveal whether the
cause was UNVERIFIED, INVALIDATED, DISABLED, or REVOKED. Trusted internal
observability may retain the reason. HARD-002 denial remains before ownership
lookup. Broken pointer/history structure remains an integrity error, not a
coarse eligibility denial.

A persisted non-COMPLETED execution identity (`PENDING`, `PROCESSING`, or
`FAILED`) or a changed completed fingerprint returns a bounded `409 Conflict`.
It is distinct from ownership ineligibility.

## Durable snapshots and schema

Completed execution snapshots are immutable. Lifecycle changes do not rewrite
them, and replay does not reevaluate them against current ownership. No
historical backfill is required. C2 uses the V50 current pointer, identity
class, ownership status, verification status, and ownership version:

```text
C2_MIGRATION_REQUIRED = NO
MIGRATION_COUNT = 50
MIGRATION_HEAD = V50
```

No V51 is part of this design.

## Compatibility and activation boundaries

Repository semantics permit LifeOS/self-linked mappings to be
`EXTERNAL / ACTIVE / UNVERIFIED`. Those mappings are structurally compatible
with V2 but are ineligible for new progression mutation until VERIFY.
Therefore:

```text
LIFEOS_REPOSITORY_COMPATIBILITY = STRUCTURALLY_COMPATIBLE
LIFEOS_RUNTIME_COMPATIBILITY = POTENTIALLY_DISRUPTIVE_FOR_ACTIVE_UNVERIFIED
LIFEOS_OPERATIONAL_DATA_STATE = UNKNOWN
```

No operational data state is inferred. Any rollout impact requires a later
explicit activation decision.

Keep these separate: code integration, runtime activation, HARD-002 grant
activation, workload HTTP activation, and LifeOS rollout. This design
authorizes none. It adds no endpoint or ingress, grant, principal, trust
record, workload activation, or LifeOS change. Enforcement belongs only on
existing progression mutation paths. Integrating C2 into a shared application
path may affect authenticated human/POC calls as well as workload calls, even
while workload HTTP is default-off; preserve native behavior and assess the
actual ingress/activation configuration separately.

## Future implementation boundary

Likely implementation universe (planning only):

**Required**

- `IdempotentExternalSubjectProgressionApplicationService`
- a new execution-eligibility application port
- a narrow persistence/JPA eligibility adapter
- `ProgressionSubjectIdentityJpaRepository`
- PostgreSQL eligibility, replay, and concurrency tests

**Likely**

- bounded C2 exception and `GlobalExceptionHandler`
- `JpaProgressionExternalExecutionStore` and/or its contract to expose
  processing status safely
- `ExternalSubjectProgressionApplicationService` if a non-idempotent external
  path remains reachable and must share C2

**Optional**

- `JpaExternalSubjectResolver`
- controller changes if existing request binding and endpoint need no behavior
  change

**Out of scope**

- HARD-002 evaluator semantics
- C1B lifecycle semantic changes
- migrations
- LifeOS
- new ingress
- grants, principals, or workload trust

## Required future tests

PostgreSQL-backed tests must cover:

- Eligibility: ACTIVE/UNVERIFIED denied; ACTIVE/VERIFIED allowed;
  ACTIVE/INVALIDATED denied; every DISABLED and REVOKED verification state
  denied; native identity unaffected.
- Currentness: pointer-selected successor used; historical ACTIVE predecessor
  ignored; missing pointer with identity/history is an integrity failure.
- Lifecycle outcomes after DISABLE, REACTIVATE, REVOKE, TRANSFER,
  REASSIGNMENT, and TARGET_CORRECTION.
- Exact COMPLETED replay after each lifecycle or verification change; changed
  fingerprint conflict.
- PENDING, PROCESSING, and FAILED are not replay; each produces bounded 409
  with no mutation. Normal transaction rollback permits retry.
- Execution races against DISABLE, REVOKE, TRANSFER, REASSIGNMENT, and
  TARGET_CORRECTION; prove pointer→identity lock order, no deadlock, and that
  the loser observes the winner's committed state.
- HARD-002 denial before ownership disclosure; coarse 403 for ownership
  ineligibility; bounded 409 for non-COMPLETED execution state.
- Native progression, HARD-001, HARD-002, all C1B lifecycle regressions, and
  historical execution snapshot immutability.

## Frozen decisions

```text
C2_EXECUTABLE_EXTERNAL_STATE = EXTERNAL / ACTIVE / VERIFIED
C2_VERIFICATION_POLICY = V2
C2_NATIVE_POLICY = NATIVE_NOT_SUBJECT_TO_EXTERNAL_VERIFICATION
C2_CURRENTNESS_SOURCE = progression_subject_current_binding
C2_ENFORCEMENT_LAYER = dedicated execution-eligibility application capability
C2_HARD002_ORDERING = authenticate → exact PROGRESSION_EXECUTE → C2 → mutation
C2_COMPLETED_REPLAY_POLICY = exact COMPLETED replay before current eligibility
C2_PENDING_POLICY = bounded 409; not replay; no mutation
C2_PROCESSING_POLICY = bounded 409; not replay; no mutation
C2_FAILED_POLICY = bounded 409; not replay or automatic external retry
C2_NORMAL_ROLLBACK_RETRY_POLICY = rollback leaves no reservation; retry allowed
C2_TWO_STAGE_IDEMPOTENCY_CHECK = initial lookup + transactional recheck after ownership locks
C2_CONCURRENCY_SERIALIZATION = shared pointer and identity locks through commit
C2_LOCK_ORDER = pointer → identity → idempotency → profile/player → mutation/snapshot
C2_OWNERSHIP_ERROR_STATUS = 403
C2_OWNERSHIP_ERROR_DISCLOSURE = coarse; lifecycle reason hidden
C2_NON_COMPLETED_EXECUTION_STATUS = 409
C2_SNAPSHOT_REWRITE_REQUIRED = NO
C2_MIGRATION_REQUIRED = NO
NEW_INGRESS = NO
NEW_AUTHORIZATION_OPERATION = NO
NEW_GRANT = NO
NEW_PRINCIPAL = NO
WORKLOAD_TRUST_CHANGE = NO
LIFEOS_CHANGE = NO
RUNTIME_ACTIVATION_INCLUDED = NO
```
