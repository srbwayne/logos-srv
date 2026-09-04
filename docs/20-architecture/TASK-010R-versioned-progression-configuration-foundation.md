# TASK-010R — Versioned Progression Configuration Foundation

Status: COMPLETE

## Decision implemented

The compatibility reference based on `AtividadeConfigId` now resolves through
immutable snapshots. `progression_configuration_definition` is the stable
logical identity and `progression_configuration_version` stores revision 1
with the configuration values and rule graph needed by the progression input.
The single `global` skill policy is represented by
`progression_skill_policy` and `progression_skill_policy_version`; its rules
are copied into `progression_skill_policy_version_rule`.

The runtime engine remains unaware of persistence and version identifiers.
`JpaProgressionConfigurationResolver` delegates to the versioned persistence
adapter, while the legacy resolver method returns the same materialized
`ProgressionConfiguration`. New `RegistroAtividade` processing stores both
version identifiers; old records remain nullable and intentionally unknown.

## Migration

`V33__Create_Versioned_Progression_Configuration.sql` creates the definition,
version, distribution, XP/stress-rule, factor snapshot, skill-policy and
skill-policy-rule tables, then adds nullable version references to
`registro_atividade`. It backfills one definition/version 1 per existing
`atividade_config` and one global policy/version 1 from the current skill
rules. It does not rewrite historical executions.

Configuration snapshots contain base XP/stress, attribute distribution keys
and weights, XP/stress rule values and cutoffs, plus factor input types.
Skill-policy snapshots contain skill key, attribute key and distribution
weight. Foreign keys use UUIDs and version uniqueness is scoped by owner and
revision. No delete cascade is used from versioned data to executions.

## Validation evidence

Validated on PostgreSQL 16.10 in isolated database `logos_task010r_validation`.
Flyway applied V1 through V32, a fixture was inserted before V32/V33, and V33
was applied through Flyway. The resulting history records version 33,
description `Create Versioned Progression Configuration`, success `true`.
The fixture produced 2 players, 2 native identity mappings, 2 configuration
definitions and 2 configuration version-1 rows; the global policy and its
version-1 row were created once each. Historical `registro_atividade` rows
were not assigned fabricated references.

The Spring processing integration tests ran against the V33 database and
confirmed successful progression behavior plus non-null configuration and
skill-policy version references on newly processed records. Compilation and
the existing progression/processing directed tests remained green. The
repository's malformed resource encoding still requires the established
`maven.resources.skip` workaround for the local directed Maven command; the
Flyway validation itself used the source migration directory directly.

## Compatibility and debts

The legacy subject/configuration contracts and HTTP endpoints are unchanged;
the engine, profile semantics, input factory numeric behavior and progression
rules are unchanged. Current version selection is an explicit pointer on the
definition/policy row, and resolution materializes the pair before execution.

Future work still includes publishing new versions on configuration/policy
edits, explicit external configuration identity/version APIs, replay,
idempotency, concurrency/optimistic locking, service authentication,
namespace authorization and eventual removal of compatibility references.
