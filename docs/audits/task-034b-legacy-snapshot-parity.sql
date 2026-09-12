-- READ ONLY.
-- This audit MUST NOT mutate or materialize progression configuration.
-- Run inside BEGIN TRANSACTION READ ONLY; and finish with ROLLBACK.

-- Result 1: summary and per-ActivityConfig classifications.
WITH
activity_state AS (
    SELECT ac.*,
           (ac.xp_base IS NOT NULL OR ac.estresse_base IS NOT NULL
            OR ac.dias_para_penalidade IS NOT NULL OR ac.xp_perda_por_ciclo IS NOT NULL
            OR EXISTS (SELECT 1 FROM regra_distribuicao_atividade rd
                       WHERE rd.atividade_config_id = ac.id)) AS has_legacy_state
    FROM atividade_config ac
),
legacy AS (
    SELECT * FROM activity_state WHERE has_legacy_state
),
defs AS (
    SELECT d.id AS definition_id, d.legacy_atividade_config_id,
           d.current_version_id, v.id AS version_found,
           v.definition_id AS version_definition_id, v.revision,
           v.base_xp, v.base_stress, v.fact_key_generation
    FROM progression_configuration_definition d
    LEFT JOIN progression_configuration_version v ON v.id = d.current_version_id
    WHERE d.legacy_atividade_config_id IS NOT NULL
),
structural AS (
    SELECT l.id, l.xp_base, l.estresse_base,
           d.definition_id, d.current_version_id, d.version_found,
           d.version_definition_id, d.revision, d.base_xp, d.base_stress,
           d.fact_key_generation,
           CASE
             WHEN d.definition_id IS NULL THEN 'NO_DEFINITION'
             WHEN d.current_version_id IS NULL THEN 'NO_CURRENT_VERSION'
             WHEN d.version_found IS NULL THEN 'BROKEN_CURRENT_VERSION_REFERENCE'
             WHEN d.version_definition_id IS DISTINCT FROM d.definition_id THEN 'VERSION_OWNER_MISMATCH'
             WHEN d.revision IS NULL OR d.revision < 1 THEN 'INVALID_REVISION'
             WHEN d.base_xp IS NULL OR d.base_stress IS NULL THEN 'BROKEN_VERSION_SCALAR'
             WHEN d.fact_key_generation IS DISTINCT FROM 'LEGACY_UUID' THEN 'UNEXPECTED_GENERATION'
             ELSE NULL
           END AS structural_reason
    FROM legacy l
    LEFT JOIN defs d ON d.legacy_atividade_config_id = l.id
),
legacy_dist AS (
    SELECT rd.atividade_config_id, rd.atributo_id::text AS attribute_key,
           rd.peso_percentual AS weight
    FROM regra_distribuicao_atividade rd
),
immutable_dist AS (
    SELECT v.id AS version_id, vd.attribute_key, vd.weight
    FROM progression_configuration_version_distribution vd
    JOIN progression_configuration_version v ON v.id = vd.configuration_version_id
),
legacy_xp AS (
    SELECT rd.atividade_config_id, rd.atributo_id::text AS attribute_key,
           rx.fator_calculo_id::text AS factor_key, rx.peso_multiplicador AS multiplier,
           rx.ponto_corte_min AS min_cutoff, rx.ponto_corte_max AS max_cutoff,
           'FIXED'::text AS calculation_mode
    FROM regra_fator_xp rx
    JOIN regra_distribuicao_atividade rd
      ON rd.id = rx.regra_distribuicao_atividade_id
),
immutable_xp AS (
    SELECT v.id AS version_id, vd.attribute_key, x.factor_key, x.multiplier,
           x.min_cutoff, x.max_cutoff, x.calculation_mode
    FROM progression_configuration_version_xp_rule x
    JOIN progression_configuration_version_distribution vd ON vd.id = x.distribution_id
    JOIN progression_configuration_version v ON v.id = vd.configuration_version_id
),
legacy_stress AS (
    SELECT rd.atividade_config_id, rd.atributo_id::text AS attribute_key,
           rs.peso_multiplicador AS multiplier, rs.ponto_corte_min AS min_cutoff,
           rs.ponto_corte_max AS max_cutoff, rs.tipo AS type
    FROM regra_fator_estresse rs
    JOIN regra_distribuicao_atividade rd
      ON rd.id = rs.regra_distribuicao_atividade_id
),
immutable_stress AS (
    SELECT v.id AS version_id, vd.attribute_key, s.multiplier, s.min_cutoff,
           s.max_cutoff, s.type
    FROM progression_configuration_version_stress_rule s
    JOIN progression_configuration_version_distribution vd ON vd.id = s.distribution_id
    JOIN progression_configuration_version v ON v.id = vd.configuration_version_id
),
live_factors AS (
    SELECT f.id::text AS factor_key, f.tipo_input FROM fator_calculo f
),
immutable_factors AS (
    SELECT v.id AS version_id, f.factor_key, f.tipo_input
    FROM progression_configuration_version_factor f
    JOIN progression_configuration_version v ON v.id = f.configuration_version_id
),
parity AS (
    SELECT s.*,
           CASE WHEN s.structural_reason IS NULL
             THEN s.xp_base IS NOT DISTINCT FROM s.base_xp
              AND s.estresse_base IS NOT DISTINCT FROM s.base_stress
             ELSE NULL END AS scalar_parity,
           CASE WHEN s.structural_reason IS NULL THEN
             NOT EXISTS ((SELECT attribute_key, weight FROM legacy_dist WHERE atividade_config_id = s.id)
                         EXCEPT
                         (SELECT attribute_key, weight FROM immutable_dist WHERE version_id = s.current_version_id))
             AND NOT EXISTS ((SELECT attribute_key, weight FROM immutable_dist WHERE version_id = s.current_version_id)
                             EXCEPT
                             (SELECT attribute_key, weight FROM legacy_dist WHERE atividade_config_id = s.id))
             ELSE NULL END AS distribution_parity,
           CASE WHEN s.structural_reason IS NULL THEN
             NOT EXISTS ((SELECT attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                          FROM legacy_xp WHERE atividade_config_id = s.id)
                         EXCEPT
                         (SELECT attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                          FROM immutable_xp WHERE version_id = s.current_version_id))
             AND NOT EXISTS ((SELECT attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                              FROM immutable_xp WHERE version_id = s.current_version_id)
                             EXCEPT
                             (SELECT attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                              FROM legacy_xp WHERE atividade_config_id = s.id))
             ELSE NULL END AS xp_parity,
           CASE WHEN s.structural_reason IS NULL THEN
             NOT EXISTS ((SELECT attribute_key, multiplier, min_cutoff, max_cutoff, type
                          FROM legacy_stress WHERE atividade_config_id = s.id)
                         EXCEPT
                         (SELECT attribute_key, multiplier, min_cutoff, max_cutoff, type
                          FROM immutable_stress WHERE version_id = s.current_version_id))
             AND NOT EXISTS ((SELECT attribute_key, multiplier, min_cutoff, max_cutoff, type
                              FROM immutable_stress WHERE version_id = s.current_version_id)
                             EXCEPT
                             (SELECT attribute_key, multiplier, min_cutoff, max_cutoff, type
                              FROM legacy_stress WHERE atividade_config_id = s.id))
             ELSE NULL END AS stress_parity,
           CASE WHEN s.structural_reason IS NULL THEN
             NOT EXISTS ((SELECT factor_key, tipo_input FROM live_factors)
                         EXCEPT
                         (SELECT factor_key, tipo_input FROM immutable_factors WHERE version_id = s.current_version_id))
             AND NOT EXISTS ((SELECT factor_key, tipo_input FROM immutable_factors WHERE version_id = s.current_version_id)
                             EXCEPT
                             (SELECT factor_key, tipo_input FROM live_factors))
             ELSE NULL END AS factor_parity,
           (SELECT COUNT(*) <> COUNT(DISTINCT (attribute_key, weight))
            FROM legacy_dist WHERE atividade_config_id = s.id)
           OR (SELECT COUNT(*) <> COUNT(DISTINCT (attribute_key, weight))
               FROM immutable_dist WHERE version_id = s.current_version_id) AS duplicate_distribution,
           (SELECT COUNT(*) <> COUNT(DISTINCT (attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode))
            FROM legacy_xp WHERE atividade_config_id = s.id)
           OR (SELECT COUNT(*) <> COUNT(DISTINCT (attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode))
               FROM immutable_xp WHERE version_id = s.current_version_id) AS duplicate_xp,
           (SELECT COUNT(*) <> COUNT(DISTINCT (attribute_key, multiplier, min_cutoff, max_cutoff, type))
            FROM legacy_stress WHERE atividade_config_id = s.id)
           OR (SELECT COUNT(*) <> COUNT(DISTINCT (attribute_key, multiplier, min_cutoff, max_cutoff, type))
               FROM immutable_stress WHERE version_id = s.current_version_id) AS duplicate_stress
    FROM structural s
),
classified AS (
    SELECT p.*,
           CASE
             WHEN p.structural_reason IN ('NO_DEFINITION', 'NO_CURRENT_VERSION')
               THEN 'NEEDS_BACKFILL'
             WHEN p.structural_reason IS NOT NULL THEN 'BROKEN_REFERENCE'
             WHEN p.duplicate_distribution OR p.duplicate_xp OR p.duplicate_stress
               THEN 'AMBIGUOUS'
             WHEN p.scalar_parity AND p.distribution_parity AND p.xp_parity
              AND p.stress_parity AND p.factor_parity
               THEN 'SAFE_FROZEN'
             ELSE 'NEEDS_BACKFILL'
           END AS classification,
           concat_ws(';',
             p.structural_reason,
             CASE WHEN p.scalar_parity = false THEN 'STALE_SCALAR' END,
             CASE WHEN p.distribution_parity = false THEN 'STALE_DISTRIBUTION' END,
             CASE WHEN p.xp_parity = false THEN 'STALE_XP_RULE' END,
             CASE WHEN p.stress_parity = false THEN 'STALE_STRESS_RULE' END,
             CASE WHEN p.factor_parity = false THEN 'STALE_FACTOR_SNAPSHOT' END,
             CASE WHEN p.duplicate_distribution OR p.duplicate_xp OR p.duplicate_stress THEN 'DUPLICATE_RULE' END
           ) AS reasons
    FROM parity p
),
summary AS (
    SELECT 'ACTIVITY_CONFIGS_TOTAL' AS metric, COUNT(*)::text AS value FROM activity_state
    UNION ALL SELECT 'LEGACY_STATE_TOTAL', COUNT(*)::text FROM legacy
    UNION ALL SELECT 'LEGACY_LINKED_DEFINITIONS', COUNT(*)::text FROM defs
    UNION ALL SELECT 'IMMUTABLE_VERSIONS_FOR_LEGACY_DEFINITIONS', COUNT(*)::text
      FROM progression_configuration_version v
      JOIN defs d ON d.definition_id = v.definition_id
    UNION ALL SELECT 'DISTRIBUTION_ROWS', COUNT(*)::text FROM regra_distribuicao_atividade
    UNION ALL SELECT 'XP_RULE_ROWS', COUNT(*)::text FROM regra_fator_xp
    UNION ALL SELECT 'STRESS_RULE_ROWS', COUNT(*)::text FROM regra_fator_estresse
    UNION ALL SELECT 'SAFE_FROZEN', COUNT(*)::text FROM classified WHERE classification = 'SAFE_FROZEN'
    UNION ALL SELECT 'NEEDS_BACKFILL', COUNT(*)::text FROM classified WHERE classification = 'NEEDS_BACKFILL'
    UNION ALL SELECT 'BROKEN_REFERENCE', COUNT(*)::text FROM classified WHERE classification = 'BROKEN_REFERENCE'
    UNION ALL SELECT 'AMBIGUOUS', COUNT(*)::text FROM classified WHERE classification = 'AMBIGUOUS'
    UNION ALL SELECT 'HISTORICAL_ONLY', '0'
    UNION ALL SELECT 'HISTORICAL_DETACHED', COUNT(*)::text
      FROM progression_configuration_definition d
     WHERE d.legacy_atividade_config_id IS NULL
       AND EXISTS (SELECT 1 FROM progression_configuration_version v WHERE v.definition_id = d.id)
    UNION ALL SELECT 'BROKEN_IMMUTABLE_SNAPSHOTS', COUNT(*)::text
      FROM classified WHERE classification = 'BROKEN_REFERENCE'
)
SELECT 'SUMMARY' AS row_type, metric, value,
       NULL::uuid AS atividade_config_id, NULL::uuid AS definition_id,
       NULL::uuid AS current_version_id, NULL::text AS reasons
FROM summary
UNION ALL
SELECT 'DETAIL', classification, NULL::text, id, definition_id,
       current_version_id, NULLIF(reasons, '')
FROM classified
ORDER BY row_type, metric, atividade_config_id;

-- Result 2: immutable detached history and durable execution references.
SELECT d.id AS definition_id,
       COUNT(DISTINCT v.id) AS immutable_version_count,
       COUNT(DISTINCT e.id) AS execution_reference_count
FROM progression_configuration_definition d
LEFT JOIN progression_configuration_version v ON v.definition_id = d.id
LEFT JOIN progression_external_execution e ON e.configuration_version_id = v.id
WHERE d.legacy_atividade_config_id IS NULL
GROUP BY d.id
ORDER BY d.id;

-- Result 3: orphan checks. Foreign keys should make these empty; the result
-- is retained to document and verify the structural assumptions explicitly.
SELECT 'distribution_without_activity' AS issue, rd.id::text AS id
FROM regra_distribuicao_atividade rd
LEFT JOIN atividade_config ac ON ac.id = rd.atividade_config_id
WHERE ac.id IS NULL
UNION ALL
SELECT 'xp_rule_without_distribution', rx.id::text
FROM regra_fator_xp rx
LEFT JOIN regra_distribuicao_atividade rd
  ON rd.id = rx.regra_distribuicao_atividade_id
WHERE rd.id IS NULL
UNION ALL
SELECT 'stress_rule_without_distribution', rs.id::text
FROM regra_fator_estresse rs
LEFT JOIN regra_distribuicao_atividade rd
  ON rd.id = rs.regra_distribuicao_atividade_id
WHERE rd.id IS NULL
ORDER BY issue, id;
