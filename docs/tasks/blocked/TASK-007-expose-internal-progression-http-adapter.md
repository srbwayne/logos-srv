# TASK-007 - Expose Internal Progression HTTP Adapter

Status: superseded by TASK-007R after TASK-008

Baseline: `1bf8522`.

## Blocker

The current `ProgressionInput` mixes event facts with Logos-owned
configuration. Facts come from numeric `RegistroAtividade` details, while
`baseXp`, `baseStress`, attribute distributions, XP/stress rules, and skill
bonus configuration come from `AtividadeConfig` and the player's skills.

There is no existing application port that resolves that configuration from a
safe activity/configuration identifier. Accepting the complete input over HTTP
would let the caller choose progression rules, violating the stateful ownership
boundary.

## Options

1. Add a configuration identifier to the request and resolve persisted Logos
   configuration internally.
2. Define a facts request tied to an existing activity/configuration and build
   `ProgressionInput` in an internal adapter. **Recommended**, because it
   follows the current `RegistroAtividade` flow and keeps rules in Logos.
3. Accept rules in the request. Rejected because it transfers rule authority to
   the caller.

No endpoint was created pending this decision. The 21-test baseline remains
green, and no schema, REST, authentication, LifeOS, or progression behavior was
changed.
