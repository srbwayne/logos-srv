# Agent Workflow

## Purpose

Agents are assistants for controlled modernization, not autonomous redesigners of Logos.

## Mandatory cycle

Every implementation task follows:

1. **Inspect** — read relevant code, tests, migrations and canonical docs.
2. **Baseline** — state current behavior and evidence before changing it.
3. **Plan** — list files, intended behavior, risks and validation.
4. **Implement** — make the smallest coherent change.
5. **Validate** — run targeted tests, then the broader applicable suite.
6. **Report** — exact files changed, tests executed, results, remaining risks and follow-up.

## Stop conditions

Stop and ask for a human decision when a task would:

- change an established domain rule without a failing test or explicit requirement;
- rewrite a capability instead of migrating it incrementally;
- delete or replace historical Flyway migrations;
- introduce a broker, service mesh, Kubernetes, distributed cache or other infrastructure not required by the current slice;
- move authentication ownership between LifeOS and Logos without an approved contract;
- make Logos depend directly on LifeOS implementation details;
- change XP/stress rounding semantics without an explicit policy and tests;
- mix unrelated cleanup with the requested slice.

## Repository safety

- Do not modify unrelated pre-existing working-tree changes.
- Do not reformat the whole repository as part of a functional task.
- Do not rewrite migration history already applied anywhere.
- Prefer additive tests before structural refactors.
- A green build does not authorize scope expansion.

## Documentation rule

If implementation contradicts canonical documentation, do not silently choose one. Report the mismatch and request a decision, or update the documentation in the same approved slice when the decision is already explicit.
