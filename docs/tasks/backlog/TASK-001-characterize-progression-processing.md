# TASK-001 — Characterize Progression Processing

## Objective

Freeze the current behavior of activity progression before architectural extraction.

## Required reading

- `docs/00-governance/AGENT-WORKFLOW.md`
- `docs/20-architecture/CURRENT-STATE.md`
- `docs/20-architecture/MIGRATION-STRATEGY.md`
- `docs/50-engine/XP-POLICY.md`
- `docs/50-engine/STRESS-POLICY.md`
- `docs/50-engine/ROUNDING-POLICY.md`

## Scope

Inspect and strengthen tests around `ProcessarRegistroAtividadeService`, `NivelXPService` and directly involved domain rules.

Characterize at least:

- XP rule matched and not matched;
- min/max cutoff boundaries;
- null cutoffs;
- percentage weight behavior;
- multiple details/factors;
- stress positive/negative behavior;
- attribute distribution;
- skill bonus;
- global and attribute level transitions;
- numeric truncation/rounding;
- processing status/result persistence.

## Forbidden

- no production refactor unless required to make an existing behavior testable and explicitly justified;
- no package moves;
- no auth changes;
- no LifeOS integration;
- no migration edits.

## Deliverable

A test-focused commit plus a report of observed rules, ambiguities and any genuine defects found. Do not fix ambiguous semantics without approval.
