# TASK-009 — External Subject Identity Boundary

## Status

BLOCKED record superseded by the approved decisions and implemented through TASK-009R.

The original blocker was the absence of decisions for namespace semantics, identity cardinality, lifecycle, onboarding, and existing-user backfill. Those decisions are now frozen in TASK-009R and ADR-0003.

## Resumption / completion status

TASK-009R implements and TASK-009R-V validates the approved CREATE and READ slice: migration/backfill, namespaced value object, explicit persistence mapping, and resolution to the internal `SubjectId` in PostgreSQL 16.10. HTTP namespaced onboarding and mapping lifecycle remain outside this slice.

Validation result: V1–V32 applied successfully in isolated databases; backfill matched all 2 pre-existing players; schema constraints and resolver passed; 37 directed tests passed. The full suite had 2 pre-existing vicio errors and no progression/identity errors.

See `TASK-009R` implementation report and `docs/70-migration/MIGRATION-LOG.md` for validation and remaining debts.
