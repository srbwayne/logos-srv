# AGENTS.md — Logos

All coding agents must begin with `docs/README.md` and follow `docs/00-governance/AGENT-WORKFLOW.md`.

## Current mission

Modernize the existing Logos codebase incrementally into an independent deterministic progression engine/service. Do not rewrite the application from scratch.

## Non-negotiable rules

- Preserve behavior before refactoring.
- Do not modify historical Flyway migrations.
- Do not overwrite or normalize unrelated pre-existing working-tree changes.
- Domain/engine must move toward independence from web, security, LifeOS and persistence implementation details.
- LifeOS is the intended first client, not a package dependency.
- Authentication/profile removal is staged; do not delete legacy functionality before an approved migration slice.
- Do not introduce infrastructure for architectural fashion.
- Stop for human approval on domain semantics or external-contract changes.

## First task

Start with `docs/tasks/backlog/TASK-001-characterize-progression-processing.md` unless the human explicitly selects another task.
