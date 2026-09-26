# ADR-0008 — Cross-System Hardening Sequence

## Status

Accepted and frozen governance sequence.

## Canonical dependency order

Gate numbers are identifiers, not topological order. The canonical sequence is:

```text
HARD-001 Workload Trust
    ↓
HARD-002 Source / Namespace / Operation Authorization
    ↓
HARD-003 External Subject Ownership Lifecycle
    ↓
HARD-005 Minimum Cross-System Correlation Contract
    ↓
HARD-004 Bounded Delivery Recovery Policy
```

Minimum cross-system correlation (HARD-005) must precede recovery architecture
(HARD-004). Do not invert HARD-005 → HARD-004.

A separate dependent branch is:

```text
HARD-001 Workload Trust
    ↓
HARD-006 Secret / Configuration Lifecycle
```

HARD-007 Deployment Boundary is deferred and productionization-required-later.

## Governance state

| Gate | Governance state |
| --- | --- |
| HARD-001 | APPROVED / FROZEN |
| HARD-002 | APPROVED / FROZEN |
| HARD-003 | OPTION B APPROVED / FROZEN |
| HARD-004 | FROZEN FOUNDATION / implementation not authorized |
| HARD-005 | FROZEN FOUNDATION / implementation not authorized |
| HARD-006 | FROZEN FOUNDATION / implementation not authorized |
| HARD-007 | DEFERRED |

## Limits of this freeze

This architecture freeze authorizes no implementation, migration, runtime
change, productization, deployment, historical replay, delivery redispatch, or
automatic recovery. In particular, do not execute `dispatch_unresolved()`,
replay, backfill, or redispatch as part of this sequence freeze.

The decisions are recorded in ADR-0005 (HARD-001), ADR-0006 (HARD-002), and
ADR-0007 (HARD-003). Gate IDs identify governance items; the dependency order
above governs sequencing.
