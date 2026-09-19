# Decision Policy

## Decision classes

### Agent-safe

Agents may decide locally when the change preserves behavior and established boundaries: naming inside a slice, private helper extraction, test fixture improvements and equivalent refactors.

### Human approval required

Human approval is required for:

- bounded-context ownership;
- public API/event contracts;
- persistence ownership and cross-service identity;
- authentication model;
- XP, level, skill, stress, vice or debuff semantics;
- synchronous versus asynchronous delivery guarantees;
- module/repository extraction;
- removal of legacy capabilities.

## ADR threshold

Create an ADR when a decision is difficult to reverse, crosses capabilities, changes an external contract, changes ownership, or introduces infrastructure.
