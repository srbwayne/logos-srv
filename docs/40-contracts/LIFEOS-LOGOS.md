# LifeOS ↔ Logos Contract

Status: **architectural contract draft; transport schema is not frozen**.

## Ownership

### LifeOS owns

- user identity and authentication;
- tenant/account context;
- personal profile;
- source facts such as books, habits, health and routines;
- canonical history of those LifeOS facts.

### Logos owns

- progression rules;
- XP calculation;
- progression levels;
- gamified attributes;
- skills and deterministic bonuses;
- gamified stress/debuff progression state retained by the Logos domain.

## Integration rule

LifeOS sends a fact plus an external subject identity. Logos evaluates progression. Logos must not query LifeOS database tables directly.

## Authentication direction

Target: Logos does not own end-user passwords/login. The concrete service-to-service/token-validation mechanism requires an ADR before implementation.

## Initial rollout

Use shadow mode. A Logos evaluation must not mutate authoritative LifeOS XP during the comparison phase.
