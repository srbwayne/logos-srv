# Test Strategy

## Priority 1 — characterization

Before moving calculations, add tests around current progression outputs. Focus on XP, stress, distribution, skill bonuses, levels, processing status and replay behavior.

## Priority 2 — pure engine tests

As policies are extracted, prefer fast unit tests with explicit inputs/outputs.

## Priority 3 — adapter/integration tests

Keep Spring/JPA/API tests for wiring, persistence and contract behavior.

## Migration gate

A structural refactor is not successful merely because it compiles. For each migrated behavior, old and new outputs must be demonstrably equivalent unless the task explicitly changes the rule.
