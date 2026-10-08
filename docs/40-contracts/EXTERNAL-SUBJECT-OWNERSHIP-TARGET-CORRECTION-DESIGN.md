# External Subject Ownership Target Correction Design

## Status and scope

Status: canonical design; runtime implementation is not authorized by this
document. This design covers only correction of a target association proven to
have been wrong when it was established. It does not change TRANSFER,
REASSIGNMENT, REVOKE, other lifecycle behavior, resolver behavior, or C2
execution enforcement.

`TARGET_CORRECTION_REQUIRED = YES`

`SELECTED_MODEL = B — successor identity for correction`

`V49_SUFFICIENT = NO`

`V50_REQUIRED = YES`

## Decision and meaning

TARGET_CORRECTION is a distinct administrative operation for a demonstrably
incorrect original target association. Examples include a wrong jogador UUID
selected during provisioning, a clerical subject association, or a
reproducible import or source-mapping defect. The operation corrects a false
historical association; it does not record a later change in who legitimately
controls the subject.

A correction requires independently reviewable evidence that the predecessor
target was incorrect from inception. The record must identify the original
provisioning/import/source assertion and the authoritative fact that
contradicts it. A reason, ticket number, or caller-provided explanation alone
does not establish that fact. If the evidence cannot distinguish an original
error from a later preference or handoff, correction is denied. A legitimate
handoff uses TRANSFER; a revoked binding recovery uses REASSIGNMENT when its
separate criteria are met.

Correction is represented by a successor identity. The predecessor remains
unchanged and historically addressable with its original target, status,
version, and history. The successor has the same external locator, a new
identity UUID, and the corrected target. A correction is not a way to edit an
identity row or erase its prior meaning.

## Comparison of candidate models

| Criterion | Model A — mutate target on same identity | Model B — correction successor | Model C — REVOKE then REASSIGNMENT | Model D — no generic correction |
| --- | --- | --- | --- | --- |
| Auditability | Requires interpreting a changed row plus history; identity appears continuous. | Explicit predecessor, successor, immutable event, and correction authorization. | Auditable as recovery, but labels an original data error as revocation/reassignment. | Prevents a generic operation; case-specific recovery remains manual and may be inconsistent. |
| Security | Unilateral in-place change can bypass TRANSFER and blur the target boundary. | Restricted to root, never-transferred, non-revoked current binding; reviewed proof and distinct executor. | Uses strong recovery controls but encourages unnecessary revocation and may hide error origin. | Lowest generic attack surface; no standard path for a proven incorrect locator. |
| Historical truth | Weak: target row is rewritten. | Strong: original target and lifecycle remain on predecessor. | Preserved, but the reason for replacement is mislabeled. | Strong if no ad hoc edits occur. |
| Schema impact | Needs correction event and authorization correlation at minimum. | Needs separate authorization, event correlation/constraints, and correction-aware successor/pointer validation. | Reuses canonical R2 schema and runtime. | None. |
| Complexity | Medium; replay and history must explain mutable target. | High; distinct immutable evidence, lineage, replay, locking, and transaction. | Low code change, high operational distortion. | Low system complexity, high case-handling burden. |
| Operational usability | Easy but too permissive. | Usable for evidence-backed inception errors. | Forces operators through semantically false REVOKE and REASSIGNMENT. | No standard correction workflow. |
| TRANSFER compatibility | Poor; easy to disguise a handoff. | Strong with proof and eligibility restrictions. | Poor; conflates error repair and recovery. | Strong. |
| REASSIGNMENT compatibility | Poor; same-row correction could mutate terminal identity. | Strong; REVOKED is explicitly ineligible and its recovery remains REASSIGNMENT. | It is REASSIGNMENT, not correction. | Strong. |
| REVOKE terminality | At risk if a revoked row is edited. | Preserved; revoked identity cannot be corrected. | Preserved by the existing flow. | Preserved. |
| Replay complexity | High because later writes mutate the same row. | High but well-defined by immutable correction event and authorization. | Existing reassignment replay applies, but the semantic event is wrong. | None for correction. |
| Concurrency complexity | Same-row lock, but competing actions and event meaning remain ambiguous. | Pointer-first and identity locks, request replay, and correction-specific CAS. | Existing reassignment coordination. | None. |
| Semantic-abuse risk | High. | Bounded by objective evidence, one correction in a lineage, no prior transfer, terminality, and reviewer/executor separation. | Medium to high through false revocation/recovery classification. | Low generic risk; risk shifts to ad hoc operations. |

**Model B is selected.** Model A is rejected because it mutates historical
identity meaning. Model C is rejected because REASSIGNMENT means recovery from
REVOKE, not correction of an erroneous initial association. Model D is safer
than an unsafe generic mutation but is not a complete policy for cases where
the original target is objectively disproven; those cases need a narrow,
auditable correction capability. Model D remains the required outcome for any
case that lacks qualifying evidence: stop and use a separately governed
case-specific process, without relabeling it as correction.

## Eligibility and state mapping

The correction candidate must be the pointer-selected current identity, be
`EXTERNAL`, be a root binding (`predecessor_identity_id IS NULL`), and have no
`OWNERSHIP_TRANSFERRED` event. It must never have been corrected already, as a
corrected identity is a successor and therefore not a root. It must not be
REVOKED. These rules prevent correction chains and prevent correction after a
legitimate handoff or a prior recovery.

ACTIVE and DISABLED bindings may be considered because an inception error can
be discovered before or after verification or temporary disablement. ACTIVE
correction is not a transfer bypass: it requires evidence of original
misassociation, a root with no transfer history, a new disabled successor,
and a separately reviewed authorization. If the actual relationship was
valid and is now changing, only TRANSFER applies. DISABLED does not relax the
same proof requirements. REVOKED is terminal and is never eligible; recovery
remains REASSIGNMENT. Native identities and `EXTERNAL / NOT_REQUIRED` are
rejected.

| Current ownership | Current verification | Correction allowed? | Result ownership | Result verification | Identity UUID changes? | Version behavior | Pointer changes? | Required evidence | History event |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ACTIVE | UNVERIFIED | Yes, if all eligibility and evidence rules pass | DISABLED | UNVERIFIED | Yes; successor UUID | Predecessor unchanged; successor version 0 | Correction-specific predecessor → successor CAS | Reviewed correction authorization plus objective inception-error record | `OWNERSHIP_TARGET_CORRECTED` |
| ACTIVE | VERIFIED | Yes, if all eligibility and evidence rules pass | DISABLED | UNVERIFIED | Yes; successor UUID | Predecessor unchanged; successor version 0 | Same | Same | Same |
| ACTIVE | INVALIDATED | Yes, if all eligibility and evidence rules pass | DISABLED | INVALIDATED | Yes; successor UUID | Predecessor unchanged; successor version 0 | Same | Same | Same |
| DISABLED | UNVERIFIED | Yes, if all eligibility and evidence rules pass | DISABLED | UNVERIFIED | Yes; successor UUID | Predecessor unchanged; successor version 0 | Same | Same | Same |
| DISABLED | VERIFIED | Yes, if all eligibility and evidence rules pass | DISABLED | UNVERIFIED | Yes; successor UUID | Predecessor unchanged; successor version 0 | Same | Same | Same |
| DISABLED | INVALIDATED | Yes, if all eligibility and evidence rules pass | DISABLED | INVALIDATED | Yes; successor UUID | Predecessor unchanged; successor version 0 | Same | Same | Same |
| REVOKED | UNVERIFIED | No | REVOKED; no operation | UNVERIFIED; unchanged | No | No change | No | None accepted for correction; use REASSIGNMENT criteria | None |
| REVOKED | VERIFIED | No | REVOKED; no operation | VERIFIED; unchanged | No | No change | No | None accepted for correction; use REASSIGNMENT criteria | None |
| REVOKED | INVALIDATED | No | REVOKED; no operation | INVALIDATED; unchanged | No | No change | No | None accepted for correction; use REASSIGNMENT criteria | None |
| ACTIVE or DISABLED | NOT_REQUIRED | No; invalid external state | No change | NOT_REQUIRED; unchanged | No | No change | No | None | None |
| REVOKED | NOT_REQUIRED | No; invalid external state and terminal | No change | NOT_REQUIRED; unchanged | No | No change | No | None | None |

Exact mapping:

```text
UNVERIFIED  -> UNVERIFIED
VERIFIED    -> UNVERIFIED
INVALIDATED -> INVALIDATED
NOT_REQUIRED -> reject
```

The corrected target requires fresh verification. The successor begins
DISABLED and version 0. The predecessor is not incremented or otherwise
mutated. No same-identity `N -> N+1` target correction exists. The pointer
remains on the predecessor until the atomic correction transaction changes it
to the successor with a correction-specific compare-and-set. No generic
pointer movement operation is permitted.

## Trusted review and authorization

Correction requires a dedicated immutable reviewed authorization with fixed
evidence type:

```text
ADMINISTRATIVE_TARGET_CORRECTION_AUTHORIZATION
```

V49's `progression_subject_reassignment_authorization` is not reusable. It is
bound to REVOKED predecessor recovery, reassignment request IDs, and
reassignment history semantics. Reusing it would conflate distinct authority
and make audit/replay claims ambiguous. The correction authorization must
bind its own authorization UUID and correction request UUID, namespace,
external ID, predecessor identity UUID/version/target, corrected target,
objective recovery basis and reviewed case/source references, reviewer
principal, and database-derived review time. Its existence means completed
review. It is immutable and has no mutable approval status or consumed flag.
Caller text is context only; it is not proof. The evidence reference is the
canonical UUID of this authorization.

The evidence package must be independently reviewable and tie the original
binding assertion to an authoritative contradictory fact. Examples include
the original provisioning record plus a signed authoritative mapping, or an
import manifest plus a deterministic migration defect record. A free-text
reason or ticket without such underlying evidence fails review. If evidence
shows that target A was valid and target B is now desired, the request is a
TRANSFER and requires bilateral consent.

Both review and execution use the existing verified `WORKLOAD` model and
`SUBJECT_OWNERSHIP_MANAGE` grant for the exact namespace. Reviewer and executor
are server-derived trusted principal IDs and must differ. No new authorization
operation, grant, principal type, or trust change is required. Authorization
must precede lookup disclosure. Review locks the current pointer and then the
selected identity, binds the exact current identity and version, and verifies
that the corrected target exists and differs from the predecessor target. It
does not mutate lifecycle state, create a successor, move the pointer, or
write a correction event.

## Command and execution contract

The future application command is conceptually:

```text
CorrectExternalSubjectTargetCommand(
    ExternalSubjectReference reference,
    UUID identityId,
    long expectedOwnershipVersion,
    UUID correctedTargetJogadorId,
    UUID correctionRequestId,
    String evidenceReference,
    String reason
)
```

The request UUID is mandatory. Reason is trimmed, nonblank, and at most 512
characters. Evidence reference must be the canonical lowercase hyphenated
authorization UUID. The caller cannot supply actor, actor type, reviewer,
provenance, event type, statuses, effective/recorded times, or authorization
state.

After exact-namespace authorization and verified-workload derivation, the
operation validates local fields and looks up immutable completed correction
history by request UUID before depending on the current pointer. Exact retry
requires the same request, normalized locator, predecessor identity/version,
corrected target, authorization, normalized reason, and recorded executor.
It returns the original semantic result. A changed payload conflicts; a
different executor conflicts. Replay is reconstructed from the immutable
correction event and authorization, never from current mutable target or
lifecycle fields. The event's successor UUID, target, statuses, version,
predecessor facts, reason, evidence, actor, provenance, request, and
authorization correlation are authoritative. Replay remains valid after
later lifecycle changes to the successor, but does not authorize another
mutation.

For a new operation, the lock and persistence sequence is:

1. authorize `SUBJECT_OWNERSHIP_MANAGE` for the exact namespace and derive the
   verified workload executor;
2. validate command syntax and perform the initial event-backed replay check;
3. load the immutable correction authorization;
4. lock the locator current pointer;
5. immediately recheck completed correction by request UUID; exact replay
   returns, mismatch conflicts;
6. lock the selected identity, then verify exact UUID, locator, root lineage,
   no prior transfer, eligible status, and expected version;
7. validate authorization bindings, reviewer/executor separation, distinct
   existing target, and evidence eligibility;
8. insert successor, append its correction event, and CAS the pointer from
   predecessor to successor;
9. commit.

All lifecycle writes continue pointer-first then identity locking. A missing
pointer with identity history remains an integrity failure. A request with no
history and no pointer follows existing not-found semantics. There is no
reverse identity-to-pointer lock and no blind retry.

Concurrent equivalent requests produce one successor, one event, and one
pointer switch; the other returns exact replay after the pointer lock and
second replay check. Same request with changed payload conflicts. Different
requests against the same predecessor, whether proposing the same or
different targets, serialize; at most one succeeds and the loser conflicts
because its predecessor is no longer current or its version/eligibility is
stale. No sleeps provide correctness.

## History and atomicity

Each successful correction writes exactly one initial successor event:

```text
OWNERSHIP_TARGET_CORRECTED
```

The event records successor identity UUID and aggregate version 0; predecessor
identity UUID/version; previous and corrected target UUIDs; previous ownership
status (`ACTIVE` or `DISABLED`) and new ownership `DISABLED`; previous and
mapped verification status; `LOGOS_OPERATOR_ACTION` provenance;
`WORKLOAD_OPERATOR` actor type and server-derived executor ID; fixed evidence
type and canonical authorization UUID reference; normalized reason;
server-derived `effective_at`; database-derived `recorded_at`; correction
request UUID; and correction authorization UUID. The event and authorization
are immutable. The event is distinct from `OWNERSHIP_TRANSFERRED` and
`OWNERSHIP_REASSIGNED`.

One transaction covers trusted authorization validation, pointer and
predecessor validation, successor insertion, event insertion, and
correction-specific pointer CAS. Any failure rolls all operation writes back;
the predecessor and authorization remain unchanged. The database target FK is
the final target-existence guard. Authorization use is evidenced by the
unique immutable event reference; no mutable consumed flag is added.

## Interaction with other lifecycle operations

- **TRANSFER:** A current identity with any `OWNERSHIP_TRANSFERRED` history is
  ineligible for correction. A transfer is an intentional handoff and cannot
  be renamed as an inception error. The correction successor is DISABLED;
  subsequent legitimate handoff still follows TRANSFER semantics.
- **REVOKE / REASSIGNMENT:** REVOKED is terminal and correction is forbidden.
  If recovery is justified, use the separate REASSIGNMENT contract and its
  reviewed authorization. Correction never revives or edits a revoked row.
- **Provisioning:** prevent target errors before the binding becomes
  operational through source validation and target confirmation. If a
  demonstrable error survives provisioning, correction remains possible for
  an eligible root in ACTIVE or DISABLED state, but always creates a disabled
  successor and requires review. This avoids making `UNVERIFIED` a proxy for
  “never operational”; it is not proof of inception error.
- **C2:** no progression-execution enforcement changes are part of this
  design. C2 remains unimplemented.

## Concurrency with lifecycle writes

Correction review and execution lock pointer then selected identity, matching
R1 and R2 ordering. All existing lifecycle writes continue to serialize on
the current pointer before identity. Outcomes are determined by lock winner
and exact version/state checks:

| Race | Required outcome |
| --- | --- |
| Correction vs VERIFY, INVALIDATE, REVERIFY, DISABLE, or REACTIVATE | One commits first. A later operation must re-read the selected identity and use the current version; eligibility is re-evaluated. No implicit retry. |
| Correction vs REVOKE | If REVOKE commits first, correction is rejected because predecessor is terminal. If correction commits first, REVOKE addressed to the predecessor is no longer current; a separately authorized operation must target the successor. |
| Correction vs TRANSFER | If TRANSFER commits first, transfer history disqualifies correction and stale version conflicts. If correction commits first, a later transfer must target the new current successor and meet existing TRANSFER requirements. |
| Correction vs REASSIGNMENT | Correction cannot start from REVOKED. If REVOKE/reassignment wins first, correction is rejected. If correction wins while predecessor is ACTIVE/DISABLED, reassignment cannot use that predecessor because it is not REVOKED/current; any later recovery must follow canonical REVOKE then REASSIGNMENT on the current successor. |
| Equivalent correction requests | One correction; other returns exact event-backed replay after pointer lock. |
| Same request, different payload or executor | Conflict; no second successor or event. |
| Different requests, same or different corrected targets | One winner for the predecessor; other conflicts after current/version recheck. |

## Database impact

V49 contains a locator-keyed current pointer, predecessor lineage, an immutable
reassignment authorization table, and reassignment-specific history
correlation and integrity guards. It does not safely encode correction as a
separate authority/event, nor do its deferred lineage and pointer guards
recognize a correction successor. Therefore:

```text
V49_SUFFICIENT = NO
V50_REQUIRED = YES
```

No migration is created here. A future V50 must, at minimum:

1. add a dedicated immutable target-correction authorization table with the
   bindings described above and one authorization per correction request;
2. add correction request and authorization correlation to ownership history,
   with event-specific null-safe checks, uniqueness, and validation trigger
   rules for `OWNERSHIP_TARGET_CORRECTED`;
3. extend deferred lineage and current-pointer validation to permit only a
   successor whose same-locator predecessor, authorization, event, and
   creation snapshots all match the correction contract;
4. preserve existing reassignment one-use checks and transfer history
   semantics, and validate all existing V49 data before changing constraints.

The migration must fail closed and must not rewrite existing history. No
expand/contract or mixed-version deployment is implied; deployment details
require the future implementation gate.

## Threat analysis

| Threat | Mitigation or disposition |
| --- | --- |
| Operator disguises TRANSFER as correction | Require objective proof of original misassociation, root identity, no transfer event, distinct review, and disabled successor. If A was valid, reject correction and require bilateral TRANSFER. |
| Forged free-text evidence | Text is context only. Require independently reviewable original-source and contradictory authoritative records; arbitrary text is rejected. |
| Compromised reviewer | Separate reviewer and executor, exact namespace authorization, immutable provenance, independent evidence, and audit. A compromised reviewer remains a residual risk; no single reviewer can execute the approved action. |
| Reviewer equals executor | Reject; both identities are server-derived and must differ. |
| Stale ownership version | Bind approval to exact current predecessor/version and recheck under pointer-first locks; stale commands conflict. |
| Stale target | Bind predecessor target and proposed target in authorization; require distinct existing target and enforce FK at commit. |
| Replay after later lifecycle mutation | Immutable correction event and authorization are replay truth; never compare mutable successor state. Require the original executor. |
| Correction after legitimate TRANSFER | Disallow if predecessor has any transfer event; subsequent correction cannot reclassify a handoff. |
| Correction after REVOKE | Disallow. REVOKED remains terminal; only canonical REASSIGNMENT may recover when separately authorized. |
| Race with REVOKE | Pointer-first lock serializes; recheck exact current identity/status/version under lock. |
| Race with TRANSFER | Pointer-first lock serializes; transfer history or current/version change makes correction ineligible/stale. |
| Race with REASSIGNMENT | REVOKED eligibility and pointer lock serialize; correction cannot act on a revoked predecessor or move a pointer generically. |
| Audit-history ambiguity | Dedicated event, evidence type, request ID, authorization table, and immutable correlation distinguish correction from transfer/reassignment. |
| Correction destroys historical truth | Successor model leaves predecessor target/status/version/history unchanged. |
| Pointer/lineage inconsistency | Correction-specific CAS plus deferred database validation binds locator, predecessor, successor, authorization, and event in one transaction. |
| Repeated corrections become de facto reassignment | Only a root with no transfer history may be corrected; the successor is not a root and cannot be corrected again. A later REVOKE recovery remains REASSIGNMENT. |
| Authorization reused across identities | Immutable authorization binds exact locator and predecessor UUID/version/target; one-use event uniqueness prevents reuse. |
| Authorization reused after lifecycle changes | Authorization binds exact predecessor version and current-state review; execution revalidates pointer, state, target, and version. Any intervening versioned operation makes it stale. |

## Frozen outcome

```text
TARGET_CORRECTION_REQUIRED = YES
SELECTED_MODEL = B — successor identity for correction
ELIGIBLE_OWNERSHIP_STATES = ACTIVE, DISABLED
REVOKED_ALLOWED = NO
VERIFICATION_MAPPING = UNVERIFIED->UNVERIFIED; VERIFIED->UNVERIFIED;
                      INVALIDATED->INVALIDATED; NOT_REQUIRED->REJECT
IDENTITY_UUID_CHANGES = YES
VERSION_BEHAVIOR = predecessor unchanged; successor version 0
POINTER_BEHAVIOR = correction-specific CAS from predecessor to successor
TRUSTED_EVIDENCE_MODEL = separate immutable reviewed authorization;
                        ADMINISTRATIVE_TARGET_CORRECTION_AUTHORIZATION
REVIEWER_EXECUTOR_SEPARATION = YES
CORRECTION_REQUEST_ID_REQUIRED = YES
REPLAY_SOURCE_OF_TRUTH = immutable OWNERSHIP_TARGET_CORRECTED event plus
                         immutable correction authorization
HISTORY_EVENT = OWNERSHIP_TARGET_CORRECTED
TRANSFER_BYPASS_PREVENTION = objective inception-error evidence; root only;
                             no prior OWNERSHIP_TRANSFERRED; reviewer != executor
REASSIGNMENT_BOUNDARY_PRESERVED = YES
REVOKE_TERMINALITY_PRESERVED = YES
CONCURRENCY_MODEL = pointer-first then identity; initial and post-lock replay;
                    one winner per predecessor; conflicts for changed payload
ATOMICITY_MODEL = authorization validation, successor, event, pointer CAS in
                  one transaction
V49_SUFFICIENT = NO
V50_REQUIRED = YES
NEW_AUTHORIZATION_OPERATION_REQUIRED = NO
NEW_GRANT_REQUIRED = NO
NEW_PRINCIPAL_REQUIRED = NO
WORKLOAD_TRUST_CHANGE_REQUIRED = NO
C2_CHANGED = NO
RUNTIME_IMPLEMENTED = NO
MIGRATION_CREATED = NO
INGRESS_ADDED = NO
```
