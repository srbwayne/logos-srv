# TASK-034B-OPS operational runbook

This audit is strictly read-only. It observes legacy ActivityConfig state and
immutable progression snapshots; it never invokes application snapshot or
backfill services.

## Canonical snapshot semantics

The current production implementation is
`JdbcVersionedProgressionConfigurationStore.snapshotConfiguration`.

| Legacy source | Immutable target | Comparison |
|---|---|---|
| `atividade_config.xp_base` | `progression_configuration_version.base_xp` | Null-safe scalar equality |
| `atividade_config.estresse_base` | `progression_configuration_version.base_stress` | Null-safe scalar equality |
| `regra_distribuicao_atividade.atributo_id`, `peso_percentual` | version distribution `attribute_key`, `weight` | Bidirectional set equality |
| `regra_fator_xp` plus distribution attribute | version XP rule tuple | Bidirectional set equality; legacy mode is `FIXED` |
| `regra_fator_estresse` plus distribution attribute | version stress rule tuple | Bidirectional set equality |
| every current `fator_calculo.id`, `tipo_input` | version factor `factor_key`, `tipo_input` | Bidirectional set equality using UUID text |

V33 initially created the legacy-linked definition and revision 1, copied the
legacy scalar/rule graph and the then-current FactDefinition input metadata,
and set `current_version_id`. V35 made the legacy XP mapping explicitly
`FIXED`; the audit therefore compares `calculation_mode = 'FIXED'` for every
legacy XP rule.

`dias_para_penalidade` and `xp_perda_por_ciclo` have no immutable snapshot
column and are not read by the current immutable materializer. They are
classified as `LEGACY_NON_RUNTIME`, so their later presence does not create a
false snapshot-parity failure. If a future runtime uses either field, the
self-sufficiency contract must be revisited before reader removal.

The SQL treats all ActivityConfigs with legacy progression state as
runtime-relevant because `resolveVersioned` and `resolveLegacyVersioned`
accept an ActivityConfig ID and may invoke the lazy fallback for it. Detached
immutable definitions are reported separately as historical data.

`SAFE_FROZEN` requires structural validity and exact set/scalar parity. Missing
or null current versions are `NEEDS_BACKFILL`; ownership/reference defects are
`BROKEN_REFERENCE`; duplicate tuples are `AMBIGUOUS`; parity mismatches are
`NEEDS_BACKFILL` with reason flags.

## Required access

Use a PostgreSQL role with no `INSERT`, `UPDATE`, `DELETE`, DDL or execute
privileges on application functions. Confirm the target environment and
database before running the audit. Do not place credentials in this file or in
the exported results.

## Procedure

1. Verify the target host, database and operator-approved environment.
2. Connect with the read-only role.
3. Verify the connected database and schema version:

```sql
SELECT current_database(), current_user, inet_server_addr(), inet_server_port();
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 1;
```

4. Start a read-only transaction:

```sql
BEGIN TRANSACTION READ ONLY;
SET LOCAL lock_timeout = '5s';
```

5. Execute `task-034b-legacy-snapshot-parity.sql` with `psql`, for example:

```text
\i docs/audits/task-034b-legacy-snapshot-parity.sql
```

6. Export the result sets with environment, database, timestamp and commit
   metadata. Preserve exact problematic IDs and reason flags.
7. Finish with:

```sql
ROLLBACK;
```

8. Close the connection.

Never invoke `snapshotConfiguration(...)`, `snapshotForResolution(...)`, a
maintenance command, or any application endpoint during this audit. Do not
repair candidates while auditing them.

## Interpretation

The operational data gate passes only when all runtime-relevant legacy
configurations are `SAFE_FROZEN` and:

```text
NEEDS_BACKFILL = 0
BROKEN_REFERENCE = 0
AMBIGUOUS = 0
```

`dias_para_penalidade` and `xp_perda_por_ciclo` are reported as legacy
non-runtime fields because the current immutable snapshot schema and runtime
materializer do not use them. They are not parity mismatches.

Detached definitions with immutable versions are historical records. They are
not backfill candidates and must not be relinked, deleted or repointed.

## Local validation

Validate the SQL against a disposable PostgreSQL database migrated from V1
through V41. Use fixtures only to exercise parsing and classification cases;
local results do not establish production completeness. The expected
structural orphan result is empty because the foreign keys in V1, V6 and V9
protect those relationships. Do not run the application resolver during this
validation because its fallback writes snapshots.
