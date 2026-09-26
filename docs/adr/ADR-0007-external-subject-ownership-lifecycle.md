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

## Audit, lifecycle, and concurrency requirements

The architecture must preserve enough information for ownership history,
actor principal, effective timestamps, transfer/correction history,
disable/revoke history, and a proof/evidence reference where applicable. The
persistence schema is undecided.

A future implementation must establish atomic semantics for: same reference
and same target; same reference and different target; claim versus disable;
claim versus transfer; and transfer versus execution. This documentation
freeze does not implement those semantics.
