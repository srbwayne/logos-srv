-- READ ONLY.
-- This audit MUST NOT mutate or materialize progression configuration.
-- Run inside BEGIN TRANSACTION READ ONLY; and finish with ROLLBACK.

-- Result 1: every existing ActivityConfig is a resolver candidate. Legacy
-- state flags are informational and do not define runtime reachability.
WITH
activity_state AS (
    SELECT ac.*,
           (ac.xp_base IS NOT NULL OR ac.estresse_base IS NOT NULL
            OR EXISTS (SELECT 1 FROM regra_distribuicao_atividade rd
                       WHERE rd.atividade_config_id = ac.id)) AS has_runtime_legacy_progression_state,
           (ac.dias_para_penalidade IS NOT NULL OR ac.xp_perda_por_ciclo IS NOT NULL)
             AS has_residual_penalty_metadata
    FROM atividade_config ac
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
    SELECT a.id, a.xp_base, a.estresse_base,
           a.has_runtime_legacy_progression_state,
           a.has_residual_penalty_metadata,
           (d.definition_id IS NULL OR d.current_version_id IS NULL
            OR d.version_found IS NULL) AS fallback_attempt,
           d.definition_id, d.current_version_id, d.version_found,
           d.version_definition_id, d.revision, d.base_xp, d.base_stress,
           d.fact_key_generation,
           CASE
             WHEN d.definition_id IS NULL
                  AND (a.xp_base IS NULL OR a.estresse_base IS NULL)
               THEN 'UNCONFIGURED_RUNTIME_CANDIDATE'
             WHEN d.definition_id IS NOT NULL AND d.current_version_id IS NULL
                  AND (a.xp_base IS NULL OR a.estresse_base IS NULL)
               THEN 'INCOMPLETE_LEGACY_BASES'
             WHEN d.definition_id IS NOT NULL AND d.current_version_id IS NULL
               THEN NULL
             WHEN d.definition_id IS NULL THEN NULL
             WHEN d.version_found IS NULL THEN 'BROKEN_CURRENT_VERSION_REFERENCE'
             WHEN d.version_definition_id IS DISTINCT FROM d.definition_id
               THEN 'VERSION_OWNER_MISMATCH'
             WHEN d.revision IS NULL OR d.revision < 1 THEN 'INVALID_REVISION'
             WHEN d.base_xp IS NULL OR d.base_stress IS NULL
               THEN 'BROKEN_VERSION_SCALAR'
             WHEN d.fact_key_generation NOT IN ('LEGACY_UUID', 'SEMANTIC')
               THEN 'UNEXPECTED_GENERATION'
             ELSE NULL
           END AS structural_reason
    FROM activity_state a
    LEFT JOIN defs d ON d.legacy_atividade_config_id = a.id
),
legacy_dist AS (
    SELECT rd.atividade_config_id, rd.atributo_id::text AS attribute_key,
           rd.peso_percentual AS weight
    FROM regra_distribuicao_atividade rd
),
immutable_dist AS (
    SELECT configuration_version_id AS version_id, attribute_key, weight
    FROM progression_configuration_version_distribution
),
legacy_xp AS (
    SELECT rd.atividade_config_id, rd.atributo_id::text AS attribute_key,
           rx.fator_calculo_id::text AS legacy_factor_key,
           f.semantic_key, rx.peso_multiplicador AS multiplier,
           rx.ponto_corte_min AS min_cutoff, rx.ponto_corte_max AS max_cutoff,
           'FIXED'::text AS calculation_mode
    FROM regra_fator_xp rx
    JOIN regra_distribuicao_atividade rd
      ON rd.id = rx.regra_distribuicao_atividade_id
    JOIN fator_calculo f ON f.id = rx.fator_calculo_id
),
immutable_xp AS (
    SELECT vd.configuration_version_id AS version_id, vd.attribute_key,
           x.factor_key, x.multiplier, x.min_cutoff, x.max_cutoff,
           x.calculation_mode
    FROM progression_configuration_version_xp_rule x
    JOIN progression_configuration_version_distribution vd
      ON vd.id = x.distribution_id
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
    SELECT vd.configuration_version_id AS version_id, vd.attribute_key,
           s.multiplier, s.min_cutoff, s.max_cutoff, s.type
    FROM progression_configuration_version_stress_rule s
    JOIN progression_configuration_version_distribution vd
      ON vd.id = s.distribution_id
),
legacy_factors AS (
    SELECT f.id::text AS legacy_factor_key, f.semantic_key, f.tipo_input
    FROM fator_calculo f
),
immutable_factors AS (
    SELECT configuration_version_id AS version_id, factor_key, tipo_input
    FROM progression_configuration_version_factor
),
parity AS (
    SELECT s.*,
           CASE WHEN s.structural_reason IS NULL AND NOT s.fallback_attempt
             THEN s.xp_base IS NOT DISTINCT FROM s.base_xp
              AND s.estresse_base IS NOT DISTINCT FROM s.base_stress
             ELSE NULL END AS scalar_parity,
           CASE WHEN s.structural_reason IS NULL AND NOT s.fallback_attempt THEN
             NOT EXISTS ((SELECT attribute_key, weight FROM legacy_dist WHERE atividade_config_id = s.id)
                         EXCEPT
                         (SELECT attribute_key, weight FROM immutable_dist WHERE version_id = s.current_version_id))
             AND NOT EXISTS ((SELECT attribute_key, weight FROM immutable_dist WHERE version_id = s.current_version_id)
                             EXCEPT
                             (SELECT attribute_key, weight FROM legacy_dist WHERE atividade_config_id = s.id))
             ELSE NULL END AS distribution_parity,
           CASE WHEN s.structural_reason IS NULL AND NOT s.fallback_attempt
                  AND s.fact_key_generation = 'LEGACY_UUID' THEN
             NOT EXISTS ((SELECT attribute_key, legacy_factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                          FROM legacy_xp WHERE atividade_config_id = s.id)
                         EXCEPT
                         (SELECT attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                          FROM immutable_xp WHERE version_id = s.current_version_id))
             AND NOT EXISTS ((SELECT attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                              FROM immutable_xp WHERE version_id = s.current_version_id)
                             EXCEPT
                             (SELECT attribute_key, legacy_factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                              FROM legacy_xp WHERE atividade_config_id = s.id))
             WHEN s.structural_reason IS NULL AND NOT s.fallback_attempt
                  AND s.fact_key_generation = 'SEMANTIC' THEN
             NOT EXISTS (SELECT 1 FROM legacy_xp
                         WHERE atividade_config_id = s.id AND semantic_key IS NULL)
             AND NOT EXISTS ((SELECT attribute_key, semantic_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                              FROM legacy_xp WHERE atividade_config_id = s.id)
                             EXCEPT
                             (SELECT attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                              FROM immutable_xp WHERE version_id = s.current_version_id))
             AND NOT EXISTS ((SELECT attribute_key, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                              FROM immutable_xp WHERE version_id = s.current_version_id)
                             EXCEPT
                             (SELECT attribute_key, semantic_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                              FROM legacy_xp WHERE atividade_config_id = s.id))
             ELSE NULL END AS xp_parity,
           CASE WHEN s.structural_reason IS NULL AND NOT s.fallback_attempt THEN
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
           CASE WHEN s.structural_reason IS NULL AND NOT s.fallback_attempt
                  AND s.fact_key_generation = 'LEGACY_UUID' THEN
             NOT EXISTS ((SELECT legacy_factor_key, tipo_input FROM legacy_factors)
                         EXCEPT
                         (SELECT factor_key, tipo_input FROM immutable_factors WHERE version_id = s.current_version_id))
             AND NOT EXISTS ((SELECT factor_key, tipo_input FROM immutable_factors WHERE version_id = s.current_version_id)
                             EXCEPT
                             (SELECT legacy_factor_key, tipo_input FROM legacy_factors))
             WHEN s.structural_reason IS NULL AND NOT s.fallback_attempt
                  AND s.fact_key_generation = 'SEMANTIC' THEN
             NOT EXISTS ((SELECT semantic_key, tipo_input FROM legacy_factors WHERE semantic_key IS NOT NULL)
                         EXCEPT
                         (SELECT factor_key, tipo_input FROM immutable_factors WHERE version_id = s.current_version_id))
             AND NOT EXISTS ((SELECT factor_key, tipo_input FROM immutable_factors WHERE version_id = s.current_version_id)
                             EXCEPT
                             (SELECT semantic_key, tipo_input FROM legacy_factors WHERE semantic_key IS NOT NULL))
             ELSE NULL END AS factor_parity,
           (SELECT COUNT(*) <> COUNT(DISTINCT (attribute_key, weight))
            FROM legacy_dist WHERE atividade_config_id = s.id)
           OR (SELECT COUNT(*) <> COUNT(DISTINCT (attribute_key, weight))
               FROM immutable_dist WHERE version_id = s.current_version_id) AS duplicate_distribution,
           (SELECT COUNT(*) <> COUNT(DISTINCT (attribute_key, legacy_factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode))
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
             WHEN p.structural_reason IN ('NO_DEFINITION', 'BROKEN_CURRENT_VERSION_REFERENCE',
                                          'VERSION_OWNER_MISMATCH', 'INVALID_REVISION',
                                          'BROKEN_VERSION_SCALAR', 'UNEXPECTED_GENERATION')
               THEN 'BROKEN_REFERENCE'
             WHEN p.structural_reason IN ('UNCONFIGURED_RUNTIME_CANDIDATE',
                                          'INCOMPLETE_LEGACY_BASES')
               THEN 'AMBIGUOUS'
             WHEN p.fallback_attempt THEN 'NEEDS_BACKFILL'
             WHEN p.duplicate_distribution OR p.duplicate_xp OR p.duplicate_stress
               OR (p.fact_key_generation = 'SEMANTIC' AND EXISTS (
                    SELECT 1 FROM legacy_xp x
                    WHERE x.atividade_config_id = p.id AND x.semantic_key IS NULL))
               THEN 'AMBIGUOUS'
             WHEN p.scalar_parity AND p.distribution_parity AND p.xp_parity
              AND p.stress_parity AND p.factor_parity
               THEN 'SAFE_FROZEN'
             ELSE 'NEEDS_BACKFILL'
           END AS classification,
           concat_ws(';',
             p.structural_reason,
             CASE WHEN p.fallback_attempt AND p.has_residual_penalty_metadata
                    THEN 'LEGACY_NON_RUNTIME' END,
             CASE WHEN p.scalar_parity = false THEN 'STALE_SCALAR' END,
             CASE WHEN p.distribution_parity = false THEN 'STALE_DISTRIBUTION' END,
             CASE WHEN p.xp_parity = false THEN 'STALE_XP_RULE' END,
             CASE WHEN p.stress_parity = false THEN 'STALE_STRESS_RULE' END,
             CASE WHEN p.factor_parity = false THEN 'STALE_FACTOR_SNAPSHOT' END,
             CASE WHEN p.duplicate_distribution OR p.duplicate_xp OR p.duplicate_stress
                    THEN 'DUPLICATE_RULE' END,
             CASE WHEN p.fact_key_generation = 'SEMANTIC' AND EXISTS (
                    SELECT 1 FROM legacy_xp x
                    WHERE x.atividade_config_id = p.id AND x.semantic_key IS NULL)
                    THEN 'SEMANTIC_KEY_UNREPRESENTABLE' END
           ) AS reasons
    FROM parity p
),
summary AS (
    SELECT 'ACTIVITY_CONFIGS_TOTAL' AS metric, COUNT(*)::text AS value FROM activity_state
    UNION ALL SELECT 'RUNTIME_RESOLVER_CANDIDATES_TOTAL', COUNT(*)::text FROM activity_state
    UNION ALL SELECT 'LEGACY_RUNTIME_STATE_TOTAL', COUNT(*)::text FROM activity_state
      WHERE has_runtime_legacy_progression_state
    UNION ALL SELECT 'PENALTY_METADATA_ROWS', COUNT(*)::text FROM activity_state
      WHERE has_residual_penalty_metadata
    UNION ALL SELECT 'SAFE_FROZEN', COUNT(*)::text FROM classified WHERE classification = 'SAFE_FROZEN'
    UNION ALL SELECT 'NEEDS_BACKFILL', COUNT(*)::text FROM classified WHERE classification = 'NEEDS_BACKFILL'
    UNION ALL SELECT 'BROKEN_REFERENCE', COUNT(*)::text FROM classified WHERE classification = 'BROKEN_REFERENCE'
    UNION ALL SELECT 'AMBIGUOUS', COUNT(*)::text FROM classified WHERE classification = 'AMBIGUOUS'
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

-- Result 2: durable references are informational, not reachability evidence.
SELECT v.id AS configuration_version_id,
       v.definition_id,
       CASE WHEN d.legacy_atividade_config_id IS NULL THEN 'DETACHED'
            ELSE 'LEGACY_LINKED' END AS legacy_link_status,
       COUNT(DISTINCT e.id) AS external_execution_reference_count,
       COUNT(DISTINCT ra.id) AS activity_execution_reference_count,
       COUNT(DISTINCT e.id) + COUNT(DISTINCT ra.id) AS total_durable_reference_count
FROM progression_configuration_version v
JOIN progression_configuration_definition d ON d.id = v.definition_id
LEFT JOIN progression_external_execution e ON e.configuration_version_id = v.id
LEFT JOIN registro_atividade ra ON ra.configuration_version_id = v.id
GROUP BY v.id, v.definition_id, d.legacy_atividade_config_id
ORDER BY v.id;

-- Result 3: structural orphan checks.
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
