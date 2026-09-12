# TASK-034B-OPS operational runbook

This audit is strictly read-only. It observes legacy ActivityConfig state and
immutable progression snapshots; it never invokes application snapshot or
backfill services. Run it inside `BEGIN TRANSACTION READ ONLY` and finish with
`ROLLBACK`.

## Runtime semantics and candidate universe

The supported resolver accepts an existing `atividade_config.id` directly.
Future runtime eligibility is therefore the complete `atividade_config` table;
historical `registro_atividade` rows are not an eligibility predicate.

The four concepts below are intentionally separate:

1. **Future runtime eligibility** — an existing ActivityConfig can be selected
   by a supported execution path.
2. **Current-version lookup success** — `findConfig` finds a definition whose
   `current_version_id` joins to a version.
3. **Live fallback attempt** — a supported `resolveVersioned` lookup misses and
   invokes `snapshotForResolution` against the live ActivityConfig.
4. **Successful lazy snapshot materialization** — the live values satisfy the
   immutable schema and `snapshotConfiguration` completes successfully.

The audit reports all ActivityConfigs. `RUNTIME_RESOLVER_CANDIDATES_TOTAL`
must equal `ACTIVITY_CONFIGS_TOTAL`. Legacy progression state and residual
penalty metadata are separate diagnostic metrics only:

- `LEGACY_RUNTIME_STATE_TOTAL` counts rows with legacy scalar or distribution
  inputs used by the snapshot comparison.
- `PENALTY_METADATA_ROWS` counts `dias_para_penalidade` or
  `xp_perda_por_ciclo`. These fields are `LEGACY_NON_RUNTIME`; they are not
  immutable progression inputs and do not cause `NEEDS_BACKFILL` by themselves.

## Fallback and empty configurations

The exact fallback-attempt condition is an ActivityConfig whose current
immutable lookup misses:

```sql
LEFT JOIN progression_configuration_definition d
  ON d.legacy_atividade_config_id = ac.id
LEFT JOIN progression_configuration_version v
  ON v.id = d.current_version_id
WHERE v.id IS NULL
```

This means a newly created, unregistered ActivityConfig can be runtime
selectable. It must not disappear from the audit. When its legacy `xp_base` or
`estresse_base` is NULL, it cannot deterministically populate the immutable
mandatory `base_xp` and `base_stress` columns. It is therefore `AMBIGUOUS` with
`UNCONFIGURED_RUNTIME_CANDIDATE` or `INCOMPLETE_LEGACY_BASES`, not an ordinary
backfill candidate. No zero/default value is inferred.

When both mandatory legacy bases exist but no usable current version exists,
the row is `NEEDS_BACKFILL`; the audit does not perform the backfill. A fallback
attempt is not evidence that lazy materialization succeeded.

## Snapshot generations and parity

Both persisted fact-key generations are valid:

- `LEGACY_UUID` uses `fator_calculo.id::text` for factor metadata and XP rules.
- `SEMANTIC` uses `fator_calculo.semantic_key` for non-NULL semantic keys.

XP and factor comparisons are generation-aware and use bidirectional exact set
equality. A semantic XP rule whose source factor has no semantic key is
`AMBIGUOUS` with `SEMANTIC_KEY_UNREPRESENTABLE`. Unknown generations are
`BROKEN_REFERENCE`. Distribution, scalar and stress comparisons remain exact;
scalar equality is NULL-aware where a comparable legacy snapshot exists.

`SAFE_FROZEN` requires a usable current immutable version, valid ownership and
revision, a supported generation, and exact applicable scalar, distribution,
XP, stress and factor parity. Stale deterministic state is `NEEDS_BACKFILL`.
Broken immutable references are `BROKEN_REFERENCE`. Duplicate tuples or state
requiring a guess are `AMBIGUOUS`.

An existing ActivityConfig with a valid immutable current version is not called
`HISTORICAL_ONLY`: it may still be selected tomorrow, but it will not require
the live legacy fallback under the current resolver. `HISTORICAL_DETACHED` is
reserved for immutable definitions with no live ActivityConfig link. Detached
history must not be relinked, repointed, rewritten or deleted.

## Durable references

The second result reports, per immutable version, the independent durable
references from `progression_external_execution.configuration_version_id` and
`registro_atividade.configuration_version_id`. These counts are informational
and never affect the primary ActivityConfig classification. Historical
immutable references must not be repointed.

## Required access and procedure

Use an operator-approved PostgreSQL role with no `INSERT`, `UPDATE`, `DELETE`,
DDL or application-function execute privileges. Confirm the target environment
and database before running the audit; do not put credentials in this file or
exported results.

1. Verify host, database and schema version (`V1` through `V41`).
2. Connect with the read-only role.
3. Execute `BEGIN TRANSACTION READ ONLY;` and set a short lock timeout.
4. Execute `docs/audits/task-034b-legacy-snapshot-parity.sql`.
5. Preserve exact IDs, classifications, reasons and environment metadata.
6. Execute `ROLLBACK;` and close the connection.

The executable SQL contains only SELECT/CTE queries. Never invoke
`snapshotConfiguration`, `snapshotForResolution`, a maintenance command or an
application endpoint during the audit.

## Summary and data gate

The summary includes:

```text
ACTIVITY_CONFIGS_TOTAL
RUNTIME_RESOLVER_CANDIDATES_TOTAL
LEGACY_RUNTIME_STATE_TOTAL
PENALTY_METADATA_ROWS
SAFE_FROZEN
NEEDS_BACKFILL
BROKEN_REFERENCE
AMBIGUOUS
HISTORICAL_DETACHED
BROKEN_IMMUTABLE_SNAPSHOTS
```

The operational data gate can pass only when `NEEDS_BACKFILL = 0`,
`BROKEN_REFERENCE = 0` and `AMBIGUOUS = 0` across the complete live
ActivityConfig candidate universe. Production completeness is not established
by CI or by this source-code correction.

## Local validation

The repository test
`LegacySnapshotParityAuditSqlTest.auditScriptExecutesReadOnlyAgainstV41Schema`
reads this file, uses PostgreSQL with a read-only connection and transaction,
executes all three result statements, and rolls back. Fixture writes, when
used by characterization tests, must occur before the read-only audit boundary.
Local PostgreSQL results validate the artifact only; they do not establish
operational data completeness.
