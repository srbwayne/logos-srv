# External Subject Ownership Target Correction — Implementation Design

## Status and authority

Status: implementation design only. TARGET_CORRECTION remains unimplemented.
This document translates the canonical [TARGET_CORRECTION semantic design](EXTERNAL-SUBJECT-OWNERSHIP-TARGET-CORRECTION-DESIGN.md)
into a proposed V50 and application contract. It does not authorize a migration,
runtime change, test change, ingress, or C2 work.

The binding semantics remain: successor identity model; only pointer-selected,
root EXTERNAL identities in ACTIVE or DISABLED state; no prior transfer or
correction successor; REVOKED and NOT_REQUIRED rejected; immutable separate
review authorization; different verified workload reviewer and executor;
successor EXTERNAL/DISABLED/version 0; correction-specific pointer CAS; and
immutable event-backed replay. No new grant, principal type, or workload trust
entry is needed.

## V50 authorization table

V50 adds `progression_subject_target_correction_authorization`. The following
contract uses the existing V49 naming and storage conventions:

| Column | Type | Nullability / rule |
| --- | --- | --- |
| `authorization_id` | UUID | Primary key; server-generated |
| `correction_request_id` | UUID | Not null; globally unique |
| `namespace` | VARCHAR(64) | Not null; nonblank after trim |
| `external_id` | VARCHAR(255) | Not null; nonblank after trim |
| `predecessor_identity_id` | UUID | Not null; restrictive composite FK to identity ID and locator |
| `predecessor_ownership_version` | BIGINT | Not null; >= 0 |
| `predecessor_target_jogador_id` | UUID | Not null; FK to `jogador(id)`, delete restricted |
| `corrected_target_jogador_id` | UUID | Not null; FK to `jogador(id)`, delete restricted; distinct from predecessor target |
| `correction_basis` | VARCHAR(2048) | Not null; trimmed and nonblank |
| `source_assertion_reference` | VARCHAR(512) | Not null; trimmed and nonblank |
| `authoritative_fact_reference` | VARCHAR(512) | Not null; trimmed and nonblank |
| `reviewed_case_reference` | VARCHAR(255) | Not null; trimmed and nonblank |
| `reviewer_principal_id` | VARCHAR(128) | Not null; trimmed and nonblank; same trusted principal representation as V49 |
| `reviewed_at` | TIMESTAMPTZ | Not null; default `clock_timestamp()` |

Add `UNIQUE (authorization_id, correction_request_id)` to support a composite
history FK. The request UUID is globally unique in this table, not scoped by
namespace. No uniqueness on predecessor is imposed on authorization rows:
multiple immutable reviews may exist for distinct requests, but only one
successor may be created from a predecessor. The existing unique lineage edge,
correction history uniqueness, current-pointer lock, and CAS enforce that
execution limit. Unused approvals remain immutable and become stale when their
bound version/currentness no longer qualifies.

The predecessor FK is `(predecessor_identity_id, namespace, external_id)` to
`progression_subject_identity(id, namespace, external_id)`, with
`ON UPDATE/DELETE RESTRICT`. Both target FKs use `ON DELETE RESTRICT`. No FK is
specified for `reviewer_principal_id`, matching the V49 representation and
avoiding a new principal lifecycle dependency. Index the unique request key;
the primary key covers authorization-ID lookup. Add a non-unique predecessor
index only if query plans for review/audit lookup require it.

No status, approved, consumed, used, or active column exists. Row existence
means review completed. A BEFORE UPDATE OR DELETE row trigger and BEFORE
TRUNCATE statement trigger reject mutation, following the V49 immutable
authorization pattern. Trigger execution must not have an application bypass.

## Evidence representation and approval capability

Evidence fields are typed logical references represented as opaque strings,
not assumed UUIDs, URLs, or database IDs. Their namespace/type is governed by
the reviewed source system; this service does not dereference external systems.
The source assertion, contradictory authoritative fact, and reviewed case are
separate mandatory references. `correction_basis` records the reviewed
explanation but does not independently establish authority. A reason or ticket
without the two source references is insufficient.

The internal capability is:

```text
ApproveExternalSubjectTargetCorrectionCommand(
    ExternalSubjectReference reference,
    UUID predecessorIdentityId,
    long expectedPredecessorOwnershipVersion,
    UUID correctedTargetJogadorId,
    UUID correctionRequestId,
    String correctionBasis,
    String sourceAssertionReference,
    String authoritativeFactReference,
    String reviewedCaseReference)
```

The caller does not provide authorization ID, reviewer, reviewed time, status,
actor, provenance, or timestamps. Normalize reference using the existing
`ExternalSubjectReference` value object. Trim text fields; reject blank values
and values exceeding the table lengths above. The repository's reason/evidence
conventions do not turn caller prose into trusted evidence.

Use the existing exact-namespace `SUBJECT_OWNERSHIP_MANAGE` authority and
verified WORKLOAD context. Approval order is: authorize namespace; derive
reviewer; validate syntax and bounds; lookup by request UUID; for a new request
lock pointer; recheck request UUID; lock selected identity; validate exact
pointer, UUID, locator, root lineage, no transfer, ACTIVE/DISABLED state,
supported verification and expected version; require distinct existing target
and objective references; insert immutable authorization; commit. The second
request lookup after pointer lock resolves an approval that committed while
this caller waited. Insert-if-absent plus reread handles uniqueness races
without continuing in a transaction marked rollback-only.

Exact request/payload/reviewer retry returns persisted authorization ID,
reviewed time, and stored facts. Changed payload or different reviewer
conflicts. Approval changes no lifecycle state, predecessor, pointer, history,
or successor. Its transaction writes only the immutable authorization row.

## V50 history extension and event constraints

Add only these correction-specific nullable columns to
`progression_subject_ownership_history`:

```text
correction_request_id UUID NULL
target_correction_authorization_id UUID NULL
```

Reuse V49 `predecessor_identity_id` and `predecessor_ownership_version` as the
general lineage/snapshot fields. Do not reuse
`reassignment_request_id` or `reassignment_authorization_id`, which retain
their reassignment-only meaning. Add an FK from the correction authorization
column to the new table and a composite FK
`(target_correction_authorization_id, correction_request_id)` to
`(authorization_id, correction_request_id)`, restrictive. Add the normal
predecessor identity FK if not already present in V49.

Use null-safe, explicit `IS NOT NULL` and `IS DISTINCT FROM` conditions in the
new event-specific CHECK. `OWNERSHIP_TARGET_CORRECTED` must require:

- correction request and authorization IDs, predecessor ID and nonnegative
  predecessor version;
- aggregate version 0; previous and new classes both `EXTERNAL`;
- previous ownership `ACTIVE` or `DISABLED`; new ownership `DISABLED`;
- previous target and new target present and distinct;
- previous verification in `UNVERIFIED`, `VERIFIED`, `INVALIDATED`; new
  verification equal to the canonical mapping (`VERIFIED` maps to
  `UNVERIFIED`, the other two retain their value);
- evidence type `ADMINISTRATIVE_TARGET_CORRECTION_AUTHORIZATION`, evidence
  reference exactly the lowercase hyphenated authorization UUID string;
- provenance `LOGOS_OPERATOR_ACTION`, actor type `WORKLOAD_OPERATOR`, nonblank
  server-derived actor ID, and nonblank normalized reason.

For every other event type, both correction-specific columns must be NULL.
Keep all existing `OWNERSHIP_REASSIGNED` conditions intact. Add partial unique
indexes for correction request ID, authorization ID, and predecessor ID where
event type is `OWNERSHIP_TARGET_CORRECTED`. Together with the existing unique
`(identity_id, aggregate_version)`, these enforce one event per request, one
event per authorization, one correction successor per predecessor, and one
version-0 creation event per successor. The existing unique identity
`predecessor_identity_id` additionally permits at most one child across
REASSIGNMENT and TARGET_CORRECTION.

The general lineage column is safe to share: a child's single version-0 event
must classify it as exactly one of `OWNERSHIP_REASSIGNED` or
`OWNERSHIP_TARGET_CORRECTED`; each event has its own authorization relation and
disjoint predecessor-state rules. Deferred lineage validation rejects missing
or dual creation classifications. Correction eligibility further requires a
root predecessor, no prior transfer, and no prior child.

## V50 database validation and pointer integrity

Extend, rather than replace semantically, the V49 deferred history, successor,
and pointer validators. Their correction branch validates the event against
the immutable authorization and both identity rows: locator equality;
predecessor ID/version/target; corrected target; request/authorization IDs;
reviewer and recorded actor; evidence type/reference; status and verification
snapshots; and successor class/status/version/lineage. The correction event
must be the successor's unique version-0 creation event. Reassignment rows
continue through the existing reassignment branch and must never be accepted
as correction rows.

Add a correction-specific pointer transition guard. At the actual pointer
UPDATE, require OLD to select the authorized predecessor and NEW to select its
validated correction successor for the same locator. The runtime CAS is
scoped to namespace, external ID, and predecessor UUID and must update exactly
one row. A deferred check confirms the final pointer selects the successor and
the event/authorization/lineage all agree. Do not add a generic pointer
movement operation. The V49 root insert behavior remains; successor insertion
does not implicitly create or move a pointer.

V50 must also protect the corrected predecessor's target, ownership,
verification, version, lineage, and history from mutation after it gains a
successor. V49 already protects revoked predecessor facts; correction can have
ACTIVE or DISABLED predecessors, so extend this protection to any identity with
a child. This is database enforcement of the canonical “predecessor unchanged”
rule, not a lifecycle status rewrite.

## Currentness and historical ACTIVE state

`CURRENTNESS_SOURCE_OF_TRUTH = locator current pointer`. A predecessor can
remain stored as ACTIVE or DISABLED after correction. Historical ACTIVE does
not mean current and does not make that identity executable; do not change its
status to manufacture inactivity. C2 remains separate.

Repository inspection found currentness selected through the pointer in
`JpaExternalSubjectResolver.resolve`,
`ProgressionSubjectIdentityJpaRepository` current-binding queries, and
`JpaSubjectOwnershipLifecycleStore.findForUpdate`. `SubjectOwnershipAggregate`
checks ACTIVE as operation eligibility after the store has selected the
current identity; that is not a currentness query. No inspected resolver or
lifecycle lookup treats `ownership_status = ACTIVE` alone as current. These
remain implementation review points; this design makes no runtime change.

## Persistence ports and application flows

Keep two narrow outbound capabilities rather than widening the general
lifecycle store:

| Port | Responsibilities |
| --- | --- |
| `TargetCorrectionAuthorizationStore` | Find immutable authorization by ID or request ID; insert-if-absent and return persisted DB-derived record. |
| `SubjectOwnershipTargetCorrectionStore` | Lock current pointer; lock selected identity by ID; detect transfer history; find completed correction projection by request ID; check target existence; insert successor; insert correction history; correction-specific pointer CAS. |

The completed correction projection contains immutable event creation facts,
authorization facts, and immutable successor locator/lineage facts if needed.
It must not require current pointer membership or use current mutable target,
status, verification, or version as replay evidence. Principal authorization
continues through the existing `AuthorizedSubjectOwnershipOperator` pattern.

The approval service follows the approval ordering above. The execution
command is:

```text
CorrectExternalSubjectTargetCommand(
    ExternalSubjectReference reference,
    UUID predecessorIdentityId,
    long expectedPredecessorOwnershipVersion,
    UUID correctedTargetJogadorId,
    UUID correctionRequestId,
    UUID targetCorrectionAuthorizationId,
    String reason)
```

Use a typed UUID for authorization rather than arbitrary evidence text; history
stores its canonical `UUID.toString()` as `evidence_reference`. Actor,
reviewer, status, event type, provenance, effective/recorded times, and
successor UUID are server/database generated.

Execution order is fixed: authorize exact namespace; derive verified WORKLOAD
executor; validate command; initial immutable event-backed replay lookup by
request; load authorization; lock current pointer; immediately recheck
completed request; resolve exact/mismatch replay before identity lock; lock the
selected identity; validate UUID, locator, root, no transfer, eligible state,
verification, and version; validate authorization bindings; require reviewer
different from executor; validate target exists and differs; generate
successor UUID; insert successor; insert version-0 event; correction-specific
pointer CAS; commit. The second replay lookup is after pointer lock and before
selected identity lock. There is no blind retry of a real transition.

## Replay and result

The immutable `OWNERSHIP_TARGET_CORRECTED` event plus immutable authorization
are the replay source of truth. Event snapshots provide predecessor and
successor creation facts, request, authorization, reason, evidence, actor, and
provenance. The identity row may supply only immutable ID, locator, and
predecessor link where needed. Never compare current `jogador_id`, ownership,
verification, or version to creation-time values. Replay does not require the
successor to remain current.

Exact replay binds request ID, authorization ID, locator, predecessor UUID and
version/target/status/verification snapshots, corrected target, successor
creation class/status/verification/version, normalized reason, fixed evidence
type/reference, provenance, actor type and original executor ID, and immutable
authorization payload/reviewer. Same request/payload/executor returns the
original semantic result. Changed payload or a different executor is a
correction-request conflict. Unknown or mismatched authorization is handled
separately as the generic trusted-authorization failure.

Reconstruct the result from event and authorization: successor ID; locator;
corrected target; initial EXTERNAL/DISABLED/mapped verification/version 0;
predecessor ID; correction request ID; and authorization ID. Do not project
current successor lifecycle state or add a replay flag.

Unknown authorization, wrong request, locator, predecessor, version, target,
or reviewer binding must be observationally equivalent at the application
boundary: one generic “target correction authorization is unavailable or does
not match” failure. Reviewer equal to executor uses the same trusted
authorization failure. Keep ordinary request replay mismatch, stale current
predecessor, expected version, ineligible state, non-root, prior transfer,
invalid verification, same target, absent target, pointer CAS, and persistence
integrity failures distinct under existing application/domain exception
conventions. Namespace authorization failures reveal no resource existence.

## Transactions, locking, and concurrency

Approval is one transaction for pointer/predecessor validation and immutable
authorization insertion only. Pointer is locked before identity. A lifecycle
mutation racing approval serializes on the pointer; approval either binds the
locked exact state or fails stale/ineligible validation. Concurrent same
request approval uses insert-if-absent and reread; one row exists and an exact
same-reviewer retry returns it.

Execution is one transaction for authorization validation, pointer and
predecessor validation, successor insert, event insert, and pointer CAS. Any
failure rolls back successor, event, and pointer; predecessor and authorization
remain unchanged. Database FK/check/trigger failures are integrity failures,
not partial success.

All competing lifecycle work follows pointer-first ordering. Same request,
same payload, same executor produces one transition and one exact replay. Same
request with changed payload or executor conflicts. Different requests for the
same predecessor—whether same or different target—yield at most one winner;
the other sees stale/non-current predecessor or lineage conflict. Correction
versus VERIFY/INVALIDATE/REVERIFY/DISABLE/REACTIVATE/REVOKE/TRANSFER serializes
at the pointer: whichever commits first determines eligibility; the loser
revalidates the now-current identity and either operates under existing rules
or conflicts. In particular, REVOKE winning makes correction ineligible;
correction winning makes the old predecessor non-current. TRANSFER winning
records transfer and advances version, so correction fails eligibility or
version. REASSIGNMENT requires revoked state and cannot validly target the
eligible predecessor; pointer serialization prevents a branch. Execution
racing authorization creation either sees the committed immutable record or
fails generically; it never consumes a partial approval. No sleeps or blind
retries establish correctness.

## Migration order and compatibility

V50 is transactional and fail-closed. It performs, in order:

1. Preflight current V49 locator/pointer coverage, identity/history references,
   lineage, and all existing reassignment event/authorization correlations.
2. Create the correction authorization table and its constraints/indexes.
3. Add row mutation and truncate protections.
4. Add nullable correction request and authorization history columns.
5. Add restrictive FKs, composite correlation FK, and unique indexes.
6. Add null-safe correction-specific event checks while retaining V49
   reassignment checks.
7. Extend history integrity validation, lineage validation, and pointer
   transition/final-state validation.
8. Extend predecessor immutability to identities with a successor.
9. Validate all existing rows against the resulting invariants, including
   existing reassignment data.

Do not rewrite history, delete data, fabricate authorization rows, backfill
correction correlations, or reseed pointers. Invalid existing V49 state aborts
the migration before it commits. Existing reassignment rows remain governed by
their current columns, checks, authorization table, and event branch; no
correction branch may classify them.

| Deployment mode | Contract |
| --- | --- |
| Old app with V50 | NOT_SUPPORTED |
| New app with V49 | NOT_SUPPORTED |
| Mixed old/new application fleet during rollout | NOT_SUPPORTED |
| Atomic deployment required | YES |

V50 introduces required event correlations and database guards that old
runtime code does not implement. New runtime code depends on V50 tables and
constraints. Deploy migration and compatible application atomically under the
repository's maintenance/rollback process; do not run mixed versions or claim
backward compatibility. A rollback after correction writes exist is not a
simple binary rollback because V49 cannot represent those records; recovery
must be a separately planned forward-compatible operation.

## Implementation slicing

Choose `MULTI_SLICE`.

**TC-R1** is a separately authorized schema/infrastructure slice: V50 DDL,
constraints/triggers/validators, persistence mappings and narrow stores needed
to validate and persist correction authorization/history, plus migration and
adapter tests. `TC_R1_EXECUTABLE_CORRECTION = NO`. It must not publish an
approval use case, executable correction command/service, successor creation,
event append, pointer CAS application path, or ingress. Database objects alone
must not let ordinary application callers perform a correction.

**TC-R2**, only after TC-R1 is canonical, adds the internal approval and
execution use cases, trusted identity handling, immutable replay, successor
creation, history, correction-specific CAS, error mapping, concurrency and
rollback tests. No HTTP, message, scheduler, or CLI ingress is included;
future ingress needs separate authorization.

## Test contract

TC-R1 migration tests cover clean V1-to-V50 and V49-to-V50, existing
reassignment preservation, malformed V49 rejection, immutable authorization
UPDATE/DELETE/TRUNCATE rejection, duplicate request rejection, invalid event
and absent request/auth correlation rejection, wrong authorization/predecessor
/version/target/status/verification/version/evidence rejection, pointer and
lineage mismatch, and ambiguous correction/reassignment successor rejection.

TC-R2 approval tests cover authorization-before-disclosure, verified workload
and exact namespace, same-request exact retry, changed payload/different
reviewer conflicts, pointer-selected root only, no transfer, ACTIVE/DISABLED
acceptance, REVOKED/NOT_REQUIRED/same-target/missing-target/stale-version
rejection, exactly one immutable row, and no lifecycle side effects.

Execution tests cover authorization ordering; initial replay and mandatory
post-pointer/pre-identity replay; generic unknown/mismatched authorization;
reviewer/executor separation; stale/non-current/non-root/prior-transfer and
state failures; all three verification mappings; exact successor, predecessor
immutability, event, authorization/request correlation and pointer; immutable
result reconstruction after later lifecycle changes; and rollback after
history, CAS, or constraint failure. Concurrency tests cover same-request
equivalent/changed-payload/different-executor, distinct requests competing for
same predecessor, approval races, lifecycle races with REVOKE/TRANSFER and
other lifecycle operations, and no branching. Use barriers/database
coordination, not timing sleeps. Retain all canonical R1/R2 regression suites;
the current baseline is 595 tests, not a promised future total.

## Threat analysis

| Threat | Mitigation |
| --- | --- |
| TRANSFER disguised as correction | Objective source contradiction, root-only/no-transfer eligibility, reviewed authorization, separate reviewer, and TRANSFER remains bilateral. |
| Forged free-text evidence or reference | Text is context only; separate typed opaque source/fact/case references; only verified reviewer can persist immutable approval. References are not independently authenticated by this service, a residual review-process trust. |
| Authorization substitution/reuse across requests or identities | Global request UUID uniqueness, exact immutable bindings, composite history FK, one-use event uniqueness, locator/predecessor/target validation, reviewer separation. |
| Request UUID reuse | Changed payload or reviewer conflicts; request UUID globally unique. |
| Reviewer equals executor or collusion | Enforce distinct verified workload principals. Two-principal collusion remains an organizational risk; preserve independent review and audit. |
| Stale approval/version/current pointer | Approval and execution lock pointer first and bind exact predecessor/version; execution revalidates before commit. |
| Historical ACTIVE mistaken for current | Pointer is currentness authority; predecessor remains unchanged; resolver/store must select pointer first. |
| Correction successor mistaken for reassignment successor, or reverse | Disjoint event type, correlation columns, authorization table/FK and validator branches; exactly one creation classification. |
| Lineage/pointer corruption or event tampering | Restrictive composite FKs, deferred validators, append-only history, immutable authorization, correction-specific immediate CAS guard and final pointer validation. |
| Mixed-version old app writes violate V50 | Old/new/mixed deployment unsupported; atomic deployment required. |
| Malformed V49 accepted during upgrade | Transactional preflight and final validation; abort without rewriting/backfilling. |
| Concurrent corrections branch | Pointer-first lock, unique predecessor lineage, request/auth/event uniqueness, one-row CAS. |
| Replay uses mutable successor | Event plus immutable authorization projection only; mutable target/status/verification/version excluded. |
| CAS without matching event, orphan successor or event | Deferred final-state validator requires successor/event/auth/pointer agreement; transaction rollback on any failure. |
| Authorization becomes stale after lifecycle change | Execution rechecks pointer, version, eligibility and exact bindings; stale approval remains immutable but cannot execute. |

## Boundaries

`HTTP_INGRESS = NO`; `MESSAGE_INGRESS = NO`; `SCHEDULER_INGRESS = NO`;
`CLI_INGRESS = NO`. No new grant, principal, or workload trust change.
`C2 = UNIMPLEMENTED`; no progression execution enforcement is specified here.
TARGET_CORRECTION remains internal-only and unimplemented until separately
authorized implementation gates complete.
