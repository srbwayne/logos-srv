# External Subject Ownership REASSIGNMENT — R2 runtime design

Status: proposed runtime contract; R2 remains unimplemented. R1 is canonical
and implemented at `ad82e08988ba7e5800c1232266df9ce0c43741e7`. This document
freezes application behavior for a future R2 implementation. It authorizes no
implementation, migration, ingress, or change to the existing lifecycle.

## 1. Scope and frozen lifecycle semantics

R2 adds two internal application capabilities: creation of a reviewed,
immutable reassignment authorization and execution of REASSIGNMENT against
that authorization. It does not add an HTTP endpoint, message consumer,
scheduler, or CLI command. Each ingress requires a separate authorization.

The canonical operation remains the V49 successor-binding model:

- Only the pointer-selected `EXTERNAL / REVOKED` binding at the exact identity
  UUID and ownership version is eligible.
- The predecessor remains permanently REVOKED and unchanged, including UUID,
  target, version, and history.
- The successor receives a server-generated UUID, the same namespace and
  external ID, a distinct target, identity class EXTERNAL, ownership DISABLED,
  version 0, and `predecessor_identity_id` set to the predecessor UUID.
- Verification maps `UNVERIFIED -> UNVERIFIED`, `VERIFIED -> UNVERIFIED`, and
  `INVALIDATED -> INVALIDATED`. `NOT_REQUIRED` is rejected.
- Exactly one `OWNERSHIP_REASSIGNED` event is inserted for the successor at
  aggregate version 0, then the current pointer moves from predecessor to
  successor in the same transaction.
- Authority remains `SUBJECT_OWNERSHIP_MANAGE` for the exact namespace through
  an authenticated, verified WORKLOAD. The audit actor is
  `WORKLOAD_OPERATOR`, with a trusted server-derived principal ID.
- Evidence type is fixed to
  `ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION`; reason is mandatory, trimmed,
  nonblank, and at most 512 characters.

TRANSFER, REVOKE, the other five implemented lifecycle operations, and their
R1 pointer-first locking behavior retain their existing semantics. This design
does not design TARGET_CORRECTION or change C2.

## 2. V49 schema fit

`V50_REQUIRED = NO`. Canonical V49 contains the structures R2 needs:

| Need | V49 structure |
| --- | --- |
| Current selection and serialization | `progression_subject_current_binding`, keyed by `(namespace, external_id)`, with a non-null unique current identity and locator-matching composite FK |
| Successor lineage | `progression_subject_identity.predecessor_identity_id`, same-locator restrictive FK, no self-link, and unique predecessor reference |
| Reviewed authorization | `progression_subject_reassignment_authorization`, including authorization UUID, unique request UUID, exact locator/predecessor/version/targets, recovery basis, reviewed case reference, reviewer principal, and DB-defaulted `reviewed_at` |
| Completion and replay | `progression_subject_ownership_history` reassignment correlation fields, event-scoped unique request and authorization indexes, strict event CHECK, and deferred null-safe validation trigger |
| Immutability | V49 UPDATE/DELETE/TRUNCATE guards on the authorization record and append-only history protections |

R2 must use these fields and constraints. It must not alter V49 or create V50.
Any discovered invariant that cannot be represented or safely enforced by
these structures blocks R2 and returns `R2_SCHEMA_BLOCKED` for a separate
design decision.

## 3. Application components and command contracts

Use the existing `core.subjectownership.application.port.in`,
`application.port.out`, and `application.service` package conventions. The
planned components are:

- `ApproveExternalSubjectReassignmentCommand`
- `ApproveExternalSubjectReassignmentUseCase`, method `approve(command)`
- `ApproveExternalSubjectReassignmentService`
- `ReassignExternalSubjectOwnershipCommand`
- `ReassignExternalSubjectOwnershipUseCase`, method `reassign(command)`
- `ReassignExternalSubjectOwnershipService`
- `SubjectOwnershipReassignmentApprovalResult` and
  `SubjectOwnershipReassignmentResult` application result records
- Narrow outbound port `SubjectOwnershipReassignmentStore`, implemented by a
  JPA adapter alongside the existing lifecycle persistence adapter

These are future class names and responsibilities, not current code.

### 3.1 Approval command

The command contains only:

```text
ExternalSubjectReference reference
UUID predecessorIdentityId
long expectedPredecessorOwnershipVersion
UUID proposedSuccessorTargetJogadorId
UUID reassignmentRequestId
String recoveryBasis
String reviewedCaseReference
```

The command does not accept authorization ID, reviewer ID, reviewed time,
evidence type, approval status, ownership status, or verification status.
Validate non-null IDs and a nonnegative version. Normalize `recoveryBasis`
with trim, require nonblank, and limit it to V49's 2048-character column.
Normalize `reviewedCaseReference` with trim, require nonblank, and limit it
to 255 characters. These strings identify and describe the review; neither is
authorization by itself.

### 3.2 Approval result

Return the persisted immutable facts:

```text
UUID authorizationId
UUID reassignmentRequestId
ExternalSubjectReference reference
UUID predecessorIdentityId
long predecessorOwnershipVersion
UUID predecessorTargetJogadorId
UUID proposedSuccessorTargetJogadorId
String reviewerPrincipalId
Instant reviewedAt
```

`reviewedAt` comes from the database default, never the command or application
clock. The authorization ID's external evidence-reference representation is
the canonical lowercase, hyphenated UUID string (`authorizationId.toString()`);
R2 accepts no alternate textual representation.

### 3.3 Reassignment command and result

The command contains only:

```text
ExternalSubjectReference reference
UUID predecessorIdentityId
long expectedPredecessorOwnershipVersion
UUID newTargetJogadorId
UUID reassignmentRequestId
String evidenceReference
String reason
```

It contains no actor, provenance, event type, status, verification result,
evidence type, or timestamp. Require non-null IDs and nonnegative version.
Normalize reason with the existing `SubjectOwnershipAdministrativeReason`
semantics: trim, require nonblank, maximum 512. `evidenceReference` must
exactly equal a canonical UUID string; do not trim or accept uppercase,
whitespace, or another UUID format. Parse it only after namespace
authorization. Unknown IDs are trusted-evidence failures, not permission to
use caller text as evidence.

The result is the same for a real transition and its exact replay:

```text
UUID successorIdentityId
ExternalSubjectReference reference
UUID targetJogadorId
OwnershipStatus ownershipStatus // DISABLED
VerificationStatus verificationStatus
long ownershipVersion // 0
UUID reassignmentRequestId
UUID authorizationId
```

Do not expose a `replayed` transport flag. Replay returns the original
successor identity and its original semantic result.

## 4. Reviewer authority and approval workflow

### 4.1 Authority order

Both services first validate only enough reference syntax to obtain its exact
namespace. They then call the existing
`SubjectOwnershipOperatorContext.authorizeForNamespace(namespace)`. That
operation both enforces `SUBJECT_OWNERSHIP_MANAGE` for the exact namespace and
requires an authenticated, verified WORKLOAD; its returned
`AuthorizedSubjectOwnershipOperator` supplies the trusted principal ID.

For approval, this principal is the reviewer. Only after authorization and
trusted context derivation may the service read request, authorization,
pointer, predecessor, or target data. For reassignment it is the executor.
No caller-supplied identity is accepted.

The separate `ApproveExternalSubjectReassignment` application operation is
the explicit attestation that the authenticated reviewer has completed the
review represented by `recoveryBasis` and `reviewedCaseReference`. Those
fields are audit context, not independent proof and not a substitute for the
verified workload or namespace grant. R2 adds no new authorization operation,
grant, principal, trust record, or seed.

### 4.2 Approval predecessor and target checks

For a new approval request, lock in this order:

1. the current-locator pointer row `FOR UPDATE`;
2. the exact identity selected by that pointer `FOR UPDATE`.

Require the selected identity UUID to equal `predecessorIdentityId`; class
EXTERNAL; ownership REVOKED; verification UNVERIFIED, VERIFIED, or INVALIDATED;
and ownership version equal to the expected version. Reject ACTIVE, DISABLED,
LOGOS_NATIVE, `logos-native`, NOT_REQUIRED, unsupported shapes, and stale
versions. Read the predecessor target from the locked row. The proposed target
must exist and differ from that target. The target FK remains the final
integrity guard; no target-row lock is required by this contract.

Approval does not move the pointer, change the predecessor, create a successor,
or append ownership history. It inserts one completed authorization row only
after all checks pass.

### 4.3 Approval request idempotency

`reassignmentRequestId` is unique in V49 and identifies one approval request.
After authorization, first look up that request ID:

- Same request UUID, exact same normalized approval payload, and same reviewer
  principal: return the existing authorization result, including its original
  authorization ID and DB timestamp.
- Same request UUID with any changed bound payload: conflict.
- Same request UUID and payload but a different reviewer: conflict because
  reviewer provenance is immutable.

For an absent request, perform pointer-first predecessor validation, then
insert using the unique request constraint. If concurrent inserts race, the
loser re-reads the committed authorization and applies the same exact payload
and reviewer comparison. It returns the existing row only for the exact retry;
it never creates a second authorization. Unique conflict resolution must not
leave the transaction unusable; use an insert-if-absent operation or an
equivalent savepoint-safe persistence strategy.

The persisted reviewer is the server-derived verified WORKLOAD principal ID.
The authorization ID is generated server-side. `reviewed_at` is DB-derived.
The V49 row's existence means review completed and authorization granted;
there is no mutable PENDING/APPROVED status. It is never updated, deleted, or
seeded. The later history event is the one-use consumption proof; there is no
consumed flag.

## 5. Reassignment authorization and replay ordering

### 5.1 Namespace authorization and local validation

The exact order is:

1. Validate/normalize the `ExternalSubjectReference` enough to identify its
   namespace.
2. Authorize `SUBJECT_OWNERSHIP_MANAGE` for that exact namespace and require
   verified WORKLOAD context.
3. Derive the executor principal ID from the returned trusted operator.
4. Normalize and validate reason.
5. Parse `evidenceReference` as a canonical UUID.
6. Only then read request history, authorization, pointer, identity, or target
   data.

Unauthorized callers receive no indication whether the locator, request, or
authorization exists.

### 5.2 First exact replay lookup

After steps 1–5, look up a completed `OWNERSHIP_REASSIGNED` event by
`reassignmentRequestId`, independent of the current pointer. V49's unique
event-scoped request index makes the result singular. The immutable
`OWNERSHIP_REASSIGNED` event is the source of truth for every creation-time
fact. Exact replay MUST NOT compare mutable current successor lifecycle fields
with the original command.

Read creation facts from the event as follows:

| Creation fact | Immutable event field |
| --- | --- |
| Successor identity UUID | `identity_id` |
| Successor target | `new_target_jogador_id` |
| Successor ownership at creation | `new_ownership_status` |
| Successor verification at creation | `new_verification_status` |
| Successor version at creation | `aggregate_version` |
| Predecessor target | `previous_target_jogador_id` |
| Predecessor ownership | `previous_ownership_status` |
| Predecessor verification | `previous_verification_status` |
| Predecessor version | `predecessor_ownership_version` |
| Reason | `reason` |
| Evidence type/reference | `evidence_type` / `evidence_reference` |
| Provenance | `provenance` |
| Actor type/executor principal | `actor_type` / `actor_id` |
| Request/authorization UUID | `reassignment_request_id` / `reassignment_authorization_id` |

The event's successor UUID, snapshot, reason, evidence, provenance, and actor
fields are immutable. The event identity's locator is obtained from immutable
identity columns (`id`, `namespace`, `external_id`); its immutable
`predecessor_identity_id` may also be checked. Those identity fields may be
used only to bind the event to the correct locator and lineage.

Do NOT use current successor `jogador_id`, `ownership_status`,
`verification_status`, or `ownership_version` to prove replay equivalence.
Valid later lifecycle operations can change those fields after reassignment.
The successor may be REACTIVATED, verified/reverified, REVOKED, or TRANSFERRED
to another target; none rewrites the original reassignment event. An exact A →
B replay still succeeds after any such valid mutation and returns the original
A → B result. The same applies after B later becomes the predecessor of B → C:
the old A → B request returns its original B result without requiring B to be
the current pointer target.

Compare the command against event creation facts and the immutable
authorization record:

- request UUID and authorization UUID/evidence reference;
- locator, predecessor UUID/version/target/ownership/verification and
  successor UUID/target/ownership/verification/version at creation;
- normalized reason, fixed evidence type, `LOGOS_OPERATOR_ACTION`,
  `WORKLOAD_OPERATOR`, and recorded executor principal;
- authorization payload, including reviewer provenance and reviewer/executor
  separation.

Exact match returns the original successor result. Any mismatch is conflict,
including a different executor principal. Current-target equality is never
replay proof. Because lookup uses the immutable request event, an old A → B
request remains replayable after a later B → C reassignment has moved the
pointer again or B's lifecycle state/target has since changed.

Reconstruct `SubjectOwnershipReassignmentResult` from the event and immutable
locator identity facts, never from mutable successor columns:

```text
successorIdentityId = event.identity_id
reference = immutable identity namespace + external_id
targetJogadorId = event.new_target_jogador_id
ownershipStatus = event.new_ownership_status // DISABLED at creation
verificationStatus = event.new_verification_status
ownershipVersion = event.aggregate_version // 0 at creation
reassignmentRequestId = event.reassignment_request_id
authorizationId = event.reassignment_authorization_id
```

Thus replay returns the original reassignment result, not the successor's
current target, status, verification, or version.

### 5.3 Real-operation path and mandatory second replay check

If the first lookup is absent:

1. Load the referenced immutable authorization after namespace authorization.
   Missing or malformed evidence is a trusted-evidence/validation failure.
2. Lock the current-locator pointer `FOR UPDATE`.
3. Immediately re-check completed history by request UUID. If another
   equivalent operation committed while this request waited, compare it using
   §5.2 and return exact replay; if the request exists with different facts,
   conflict.
4. Lock the pointer-selected identity row `FOR UPDATE`.
5. Require pointer-selected UUID to equal the command predecessor UUID;
   require EXTERNAL / REVOKED, supported verification, exact predecessor
   version, and the authorization's recorded predecessor target.
6. Compare the complete authorization against request, exact locator,
   predecessor UUID/version/target, and proposed target. Require reviewer ID
   different from executor ID.
7. Require the new target exists and differs from predecessor target.
8. Create the successor, append its initial event, compare-and-switch the
   pointer, then commit.

The second lookup is mandatory: equivalent concurrent callers can both miss
the first lookup, but after pointer-lock serialization the later caller must
see the committed request event and return its exact result instead of
incorrectly reporting a stale predecessor.

## 6. Persistence port and transaction boundary

Use a narrow `SubjectOwnershipReassignmentStore` port; do not reuse ambiguous
locator lookup APIs or expose generic pointer movement. Its conceptual
operations are:

```text
findAuthorizationById(authorizationId)
findAuthorizationByRequestId(reassignmentRequestId)
insertAuthorizationIfAbsent(authorizationRecord)
findCompletedReassignmentByRequestId(reassignmentRequestId)
lockCurrentPredecessor(reference) // pointer first, identity second
targetExists(targetJogadorId)
completeReassignment(predecessor, successor, event, expectedPointerIdentityId)
```

`completeReassignment` is specific to reassignment. Within the service's
single transaction it inserts the successor row, inserts exactly one
`OWNERSHIP_REASSIGNED` history row with all V49 correlation fields, then runs
the pointer compare-and-switch. It updates the pointer only where locator
matches and `current_identity_id = predecessorIdentityId`; exactly one row
must change. Zero rows is a conflict/integrity failure. The DB's deferred V49
constraints validate the final successor, event, authorization, and pointer
relationship at commit.

The successor is server-created with a new UUID, same locator, EXTERNAL,
DISABLED, version 0, selected target, mapped verification, and predecessor
link. The predecessor is not updated. Event identity is the successor UUID;
event aggregate version is 0. Event fields include predecessor UUID/version,
request UUID, authorization UUID, complete before/after snapshots, fixed
evidence type and canonical authorization UUID string, normalized reason,
operator provenance/actor, server-derived `effective_at`, and DB-derived
`recorded_at`.

The transaction covers authorization validation, successor insert, history
insert, and pointer CAS. Any validation, FK, unique, history-trigger, or CAS
failure rolls back successor, history, and pointer; predecessor and immutable
authorization remain unchanged. Do not automatically retry a real transition.

### Persistence responsibilities

- `findAuthorizationById`: exact UUID lookup, invoked only after namespace
  authorization.
- `findAuthorizationByRequestId`: approval idempotency lookup, only after
  namespace authorization.
- `findCompletedReassignmentByRequestId`: event + successor + authorization
  projection containing all immutable event facts needed for exact matching
  and result reconstruction, plus immutable authorization facts. It does not
  require current-pointer membership or mutable successor lifecycle state.
  The associated identity row, if queried, supplies only immutable identity,
  locator, and predecessor-link values; current target/status/verification/
  version are excluded from replay evidence.
- `lockCurrentPredecessor`: pointer lock followed by the selected identity
  lock and locator/current confirmation.
- `targetExists`: read-only target check; the target FK remains authoritative.
- `insertAuthorizationIfAbsent`: one immutable insert, resolving unique
  request races by re-reading and exact comparison.
- `completeReassignment`: insert successor and event then reassignment-only
  pointer CAS in one transaction. No generic pointer update is exposed.

## 7. Concurrency contract

| Requests | Required outcome |
| --- | --- |
| Same request, same payload, same executor | One transition, one successor, one event; the other caller returns exact replay. |
| Same request, different payload | One committed result at most; the other caller conflicts. |
| Same request and payload, different executor | Conflict; replay cannot impersonate the recorded executor. |
| Different requests, same predecessor and same target | One transition; the other conflicts because its predecessor is no longer current. |
| Different requests, same predecessor and different targets | One winner and one conflict; never two current successors. |
| Approval retries with same request/payload/reviewer | One authorization row; retries return its original facts. |
| Approval retries with changed payload or reviewer | Conflict; no second authorization row. |

All lifecycle writes follow authorization → pointer lock → current identity
lock. Approval uses this same lock order. REASSIGNMENT cannot race a valid
REACTIVATE or TRANSFER from a REVOKED predecessor because those operations
continue to reject REVOKED; pointer locking still serializes the attempts.
For a later chain, B must independently become REVOKED before a new reviewed
authorization can create C. A remains historical and unchanged.

## 8. Failure and disclosure semantics

R2 has no transport ingress, so this section defines application outcomes, not
HTTP status codes. Use existing conventions where possible:

| Condition | Application outcome |
| --- | --- |
| Namespace authorization denied or workload unverified | Existing generic `AccessDeniedException`; do not read or disclose resources. |
| Missing current predecessor after authorization | `SubjectOwnershipIdentityNotFoundException` with its generic message. |
| Invalid/blank/oversized reason or noncanonical evidence UUID | Input validation failure (`IllegalArgumentException`/domain value validation); no persistence reads before the authorization boundary. |
| ACTIVE, DISABLED, native, NOT_REQUIRED, malformed state, or same target | `InvalidSubjectOwnershipTransitionException`. |
| Stale predecessor version | `SubjectOwnershipVersionConflictException`. |
| Existing predecessor is no longer pointer-selected | Reassignment conflict; do not disclose or mutate the newer current binding. |
| Missing authorization or authorization facts not bound to the command | One generic trusted-authorization failure; do not expose which bound field differed. |
| Reviewer and executor are the same principal | Generic trusted-authorization failure; the two-person rule is mandatory. |
| Approval request UUID reused with changed payload/reviewer | Reassignment conflict. |
| Completed request UUID reused with changed payload or executor | Reassignment conflict. |
| New target does not exist | `InvalidSubjectOwnershipTransitionException`; the target FK remains the final guard. |
| Pointer CAS changes zero or more than one row | Conflict/integrity failure; transaction rolls back. |
| Constraint, trigger, FK, or persistence integrity failure | Persistence failure; transaction rolls back and the immutable authorization remains. |

Use one R2 conflict type (for example,
`SubjectOwnershipReassignmentConflictException`) for idempotency mismatch,
request reuse, and stale-current/CAS races. Use one generic authorization
failure type for unknown authorization and bound-payload mismatch. No adapter
may distinguish those authorization cases to an unauthorized caller.

## 9. Test plan for a later R2 implementation

### Approval

- authorized reviewer creates a record with server UUID and DB `reviewed_at`;
- record binds exact current pointer, predecessor UUID/version/target, and
  distinct existing target;
- ACTIVE, DISABLED, native, NOT_REQUIRED, stale version, same target, and
  absent target are rejected;
- exact same-request/payload/reviewer retry returns original authorization;
- changed payload or different reviewer for the same request conflicts;
- concurrent exact approval creation leaves one immutable row;
- UPDATE, DELETE, and TRUNCATE remain rejected.

### Real reassignment and rollback

- UNVERIFIED, VERIFIED, and INVALIDATED map exactly as specified;
- predecessor row and history remain unchanged;
- successor UUID, DISABLED status, version 0, locator, target, lineage, and
  pointer are exact;
- exactly one version-0 `OWNERSHIP_REASSIGNED` event has V49 correlation,
  snapshots, evidence, actor, reason, and timestamp provenance;
- malformed/unknown evidence, mismatched authorization, same reviewer and
  executor, stale/non-current predecessor, ACTIVE/DISABLED/native/
  NOT_REQUIRED, same target, and missing target are rejected;
- injected history failure and pointer CAS failure roll back successor,
  history, and pointer; predecessor/authorization remain unchanged.

### Exact replay

- exact replay returns the original successor;
- replay still returns A → B's original result after the current pointer has
  advanced to C;
- exact replay succeeds after successor REACTIVATE;
- exact replay succeeds after successor VERIFY/REVERIFY where applicable;
- exact replay succeeds after successor REVOKE;
- exact replay succeeds after successor TRANSFER;
- replay result retains original creation target, DISABLED status, verification,
  and version 0 rather than projecting later mutable successor state;
- exact A → B replay succeeds after a later B → C chain and returns original B;
- changed target, evidence, reason, predecessor/version, authorization, or
  executor conflicts;
- current target alone never satisfies replay.

### Concurrency and regression

- equivalent concurrent request yields one transition plus one exact replay;
- same request with different payload/executor yields one winner and conflict;
- different requests against one predecessor, for same or competing targets,
  yield one winner and one conflict;
- approval and reassignment use pointer-first locks without reverse ordering;
- all existing R1 lifecycle and pointer infrastructure regressions remain
  green (baseline 532 tests; do not assume a future total).

## 10. Explicit boundaries

```text
APPROVAL_HTTP_INGRESS_IN_R2      = NO
APPROVAL_MESSAGE_INGRESS_IN_R2   = NO
APPROVAL_SCHEDULER_INGRESS_IN_R2 = NO
APPROVAL_CLI_INGRESS_IN_R2       = NO
HTTP_INGRESS_IN_R2               = NO
MESSAGE_INGRESS_IN_R2            = NO
SCHEDULER_INGRESS_IN_R2          = NO
CLI_INGRESS_IN_R2                = NO
V50_REQUIRED                     = NO
TARGET_CORRECTION_DESIGNED       = NO
C2_CHANGED                       = NO
```

REASSIGNMENT and its reviewed authorization are internal application
capabilities only. No grants, principals, trust records, workloads, or
authorization operations are created by this design. The implementation must
not proceed if it cannot enforce reviewer/executor separation using trusted
WORKLOAD identities and canonical V49 persistence.
