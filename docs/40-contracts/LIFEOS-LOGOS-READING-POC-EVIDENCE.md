# LifeOS → Logos Reading POC Evidence

Status: SUCCESS / VERIFIED / PRE-RELEASE POC.

## 1. Scope

This records the first bounded real LifeOS → Logos Reading progression POC and
its subsequent idempotency recovery check. It is evidence of one controlled
integration path, not a production-readiness claim.

## 2. Canonical baselines

| System | Source baseline | CI |
| --- | --- | --- |
| LifeOS | `db608b3230e72f07acb496d5c1ad466b1b276810` | `35411604626` — SUCCESS |
| Logos | `02ccad4b8cb17d9a2bc847ec007ab12f19542e4c` | `35486545748` — SUCCESS |

The Logos operational schema was Flyway V43. The experiment did not modify
either source repository.

## 3. Frozen contract

```text
subject.namespace: lifeos
subject.externalId: 0RAX3EN3SVK2Z
execution.source: lifeos
execution.idempotencyKey: reading-session:0RNXGW8J67V68
configuration.key: reading
configuration.revision: 1
factorKey: pages_read
value: 3
```

The resolved Logos configuration version was
`70a80857-4b59-4cde-9a8d-e45452820880`.

## 4. LifeOS evidence

| Item | Identifier / value |
| --- | --- |
| Book | `0RNXGW86RWC70` — *LifeOS-Logos Reading POC 001* by *LifeOS Integration Fixture*; 10 pages |
| ReadingSession | `0RNXGW8J67V68` — owner `0RAX3EN3SVK2Z`, pages 1–3, `pages_read = 3` |
| BookCompletion | No |
| Progression delivery | `0RNXGW8JCPZ17` — DELIVERED, attempt count 1, last error NULL, recoverable NO, unresolved NO |

The bounded POC did not call `dispatch_unresolved()` and did not replay
unrelated historical unresolved deliveries.

## 5. Logos evidence

The `lifeos / 0RAX3EN3SVK2Z` mapping resolved to the dedicated POC subject:

```text
AppUser: 15ecf744-c1a2-480c-b981-0cbd741d933d
Jogador: 67ddefe2-e333-4029-9382-5f72d2dd00e1
```

The active `reading` revision 1 used `pages_read` in `FACT_VALUE` mode, base
XP 1, a 1.0 distribution to **Conhecimento**, multiplier 1.0, and no stress
rules.

## 6. Expected versus observed

Input: `pages_read = 3`.

| Evidence | Before | After |
| --- | ---: | ---: |
| `progression_external_execution` | 0 | 1 |
| Global XP | 0 | 3 |
| Conhecimento XP | 0 | 3 |
| Stress | 0 | 0 |

All expected deltas were confirmed.

## 7. Durable execution

LifeOS produced a ReadingSession, derived `pages_read = 3`, created the
delivery, and sent the supported Logos progression request. Logos resolved the
namespaced subject and immutable Reading revision, persisted one durable
external execution, and LifeOS reconciled the delivery as DELIVERED.

## 8. Idempotency proof

Before recovery, exact execution readback and subject history both returned
HTTP 200; history reported one logical execution. One authorized identical
resubmit then used the same subject, source, idempotency key, configuration,
revision, factor, and value.

The resubmit returned HTTP 200. After it, the durable execution count remained
1, global XP remained 3, Conhecimento XP remained 3, stress remained 0, and
history still reported one logical execution. Two known identical requests
therefore produced no duplicate progression.

## 9. Historical-delivery exclusion

Unrelated pre-existing unresolved LifeOS deliveries were deliberately excluded
from both the POC and recovery. No automatic historical replay was performed.

## 10. Original verifier defect

The original `LIFEOS-LOGOS-001D` verifier retained a hard stop after the
mutation point because its local verification harness raised
`KeyError: POC_BOOK_ID`. This is classified as a **LOCAL VERIFIER / HARNESS
DEFECT**, not as a LifeOS failure, Logos failure, integration failure, or data
corruption.

## 11. Recovery result

The later dedicated recovery gate verified committed state and idempotency:

```text
RECOVERY: PASS
RECOVERY PENDING: NO
NEW 001D EXECUTION: PROHIBITED
```

## 12. Security handling

Runtime JWTs and runtime signing secrets were not persisted. POC credential
material remains outside the repository. No repository secret was introduced.

## 13. What this POC proves

This bounded experiment provides operational evidence that LifeOS can create a
ReadingSession, derive the factor, create and reconcile a delivery, and call
the supported Logos contract. It also proves resolution of the namespaced
subject and Reading revision 1, durable execution persistence, +3 global XP,
+3 Conhecimento XP, zero stress, exact readback, subject history, and safe
identical resubmission.

## 14. What this POC does not prove

It does not establish production-grade service-to-service authentication,
source or namespace authorization, production secret distribution, production
retry scheduling, automatic historical replay, general concurrent same-subject
execution guarantees, semantic Attribute identity in public progression
responses, scientific validity of one page equalling one XP, behavior for all
LifeOS activities, or production deployment readiness.

## 15. Follow-up work

The display attribute remains **Conhecimento**. The planned future semantic
machine identity is `knowledge`, but current Progression HTTP V1 attribute key
fields are UUID-based. `Atributo.semanticKey`, V44, attribute-key generations,
and response changes are not part of this POC. `LOGOS-ATTR-IDENTITY-001-F1`
remains queued for reissue after this closeout is independently reviewed.
