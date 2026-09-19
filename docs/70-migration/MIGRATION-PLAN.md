# Migration Plan

## M0 — Documentation and safety baseline

Goal: establish canonical docs and agent workflow without changing production behavior.

Exit: docs committed separately; repository baseline recorded.

## M1 — Progression characterization

Add/strengthen tests for `ProcessarRegistroAtividadeService` and adjacent level/rule behavior. Record exact numeric semantics and edge cases.

## M2 — Extract XP policy

Move XP arithmetic behind a domain/engine policy while preserving output exactly.

## M3 — Extract stress policy

Move stress arithmetic behind a policy while preserving output exactly.

## M4 — Extract distribution and skill bonus policies

Separate attribute distribution and skill bonus evaluation from orchestration.

## M5 — Introduce ProgressionEngine

Compose proven policies into an explicit engine result. Keep existing activity-processing flow as an adapter/client of the engine.

## M6 — Decouple progression subject from legacy profile/auth

Introduce external subject identity at the progression boundary. Do not delete legacy auth/profile yet.

## M7 — LifeOS pilot contract

Inspect the selected LifeOS vertical and define a versioned internal contract. Implement shadow evaluation only.

## M8 — Shadow comparison

Collect comparable LifeOS versus Logos results and define acceptance criteria.

## M9 — Authority cutover

Only after explicit approval, make Logos authoritative for the selected progression calculation.

## M10 — Legacy removal / physical modularization

Remove obsolete auth/profile responsibilities and consider Maven module extraction only after consumers and dependency boundaries are stable.
