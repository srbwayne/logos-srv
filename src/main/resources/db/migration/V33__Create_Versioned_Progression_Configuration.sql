-- TASK-010R: immutable configuration and global skill policy snapshots.
CREATE TABLE progression_configuration_definition (
    id UUID PRIMARY KEY,
    logical_key VARCHAR(255) NOT NULL UNIQUE,
    legacy_atividade_config_id UUID UNIQUE,
    current_version_id UUID,
    CONSTRAINT fk_progression_configuration_definition_legacy
        FOREIGN KEY (legacy_atividade_config_id) REFERENCES atividade_config(id) ON DELETE SET NULL
);

CREATE TABLE progression_configuration_version (
    id UUID PRIMARY KEY,
    definition_id UUID NOT NULL,
    revision INT NOT NULL,
    base_xp INT NOT NULL,
    base_stress INT NOT NULL,
    CONSTRAINT uq_progression_configuration_version_revision UNIQUE (definition_id, revision),
    CONSTRAINT fk_progression_configuration_version_definition
        FOREIGN KEY (definition_id) REFERENCES progression_configuration_definition(id)
);

ALTER TABLE progression_configuration_definition
    ADD CONSTRAINT fk_progression_configuration_definition_current
    FOREIGN KEY (current_version_id) REFERENCES progression_configuration_version(id);

CREATE TABLE progression_configuration_version_distribution (
    id UUID PRIMARY KEY,
    configuration_version_id UUID NOT NULL,
    attribute_key VARCHAR(36) NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_progression_configuration_version_distribution_version
        FOREIGN KEY (configuration_version_id) REFERENCES progression_configuration_version(id)
);

CREATE TABLE progression_configuration_version_xp_rule (
    id UUID PRIMARY KEY,
    distribution_id UUID NOT NULL,
    factor_key VARCHAR(36) NOT NULL,
    multiplier DOUBLE PRECISION,
    min_cutoff DOUBLE PRECISION,
    max_cutoff DOUBLE PRECISION,
    CONSTRAINT fk_progression_configuration_version_xp_rule_distribution
        FOREIGN KEY (distribution_id) REFERENCES progression_configuration_version_distribution(id)
);

CREATE TABLE progression_configuration_version_stress_rule (
    id UUID PRIMARY KEY,
    distribution_id UUID NOT NULL,
    multiplier DOUBLE PRECISION NOT NULL,
    min_cutoff DOUBLE PRECISION,
    max_cutoff DOUBLE PRECISION,
    type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_progression_configuration_version_stress_rule_distribution
        FOREIGN KEY (distribution_id) REFERENCES progression_configuration_version_distribution(id)
);

CREATE TABLE progression_configuration_version_factor (
    id UUID PRIMARY KEY,
    configuration_version_id UUID NOT NULL,
    factor_key VARCHAR(36) NOT NULL,
    tipo_input VARCHAR(50) NOT NULL,
    CONSTRAINT uq_progression_configuration_version_factor UNIQUE (configuration_version_id, factor_key),
    CONSTRAINT fk_progression_configuration_version_factor_version
        FOREIGN KEY (configuration_version_id) REFERENCES progression_configuration_version(id)
);

CREATE TABLE progression_skill_policy (
    id UUID PRIMARY KEY,
    logical_key VARCHAR(255) NOT NULL UNIQUE,
    current_version_id UUID
);

CREATE TABLE progression_skill_policy_version (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL,
    revision INT NOT NULL,
    CONSTRAINT uq_progression_skill_policy_version_revision UNIQUE (policy_id, revision),
    CONSTRAINT fk_progression_skill_policy_version_policy
        FOREIGN KEY (policy_id) REFERENCES progression_skill_policy(id)
);

ALTER TABLE progression_skill_policy
    ADD CONSTRAINT fk_progression_skill_policy_current
    FOREIGN KEY (current_version_id) REFERENCES progression_skill_policy_version(id);

CREATE TABLE progression_skill_policy_version_rule (
    id UUID PRIMARY KEY,
    policy_version_id UUID NOT NULL,
    skill_key VARCHAR(36) NOT NULL,
    attribute_key VARCHAR(36) NOT NULL,
    distribution_weight DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_progression_skill_policy_version_rule_version
        FOREIGN KEY (policy_version_id) REFERENCES progression_skill_policy_version(id)
);

ALTER TABLE registro_atividade
    ADD COLUMN configuration_version_id UUID,
    ADD COLUMN skill_policy_version_id UUID,
    ADD CONSTRAINT fk_registro_atividade_configuration_version
        FOREIGN KEY (configuration_version_id) REFERENCES progression_configuration_version(id),
    ADD CONSTRAINT fk_registro_atividade_skill_policy_version
        FOREIGN KEY (skill_policy_version_id) REFERENCES progression_skill_policy_version(id);

-- One definition and version 1 for every current legacy configuration.
INSERT INTO progression_configuration_definition (id, logical_key, legacy_atividade_config_id)
SELECT gen_random_uuid(), 'legacy:' || id::text, id
FROM atividade_config;

INSERT INTO progression_configuration_version (id, definition_id, revision, base_xp, base_stress)
SELECT gen_random_uuid(), d.id, 1, c.xp_base, c.estresse_base
FROM progression_configuration_definition d
JOIN atividade_config c ON c.id = d.legacy_atividade_config_id;

UPDATE progression_configuration_definition d
SET current_version_id = v.id
FROM progression_configuration_version v
WHERE v.definition_id = d.id AND v.revision = 1;

CREATE TEMP TABLE progression_configuration_distribution_backfill (
    legacy_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    version_id UUID NOT NULL,
    attribute_key VARCHAR(36) NOT NULL,
    weight DOUBLE PRECISION NOT NULL
) ON COMMIT DROP;

INSERT INTO progression_configuration_distribution_backfill (legacy_id, snapshot_id, version_id, attribute_key, weight)
SELECT r.id, gen_random_uuid(), v.id, r.atributo_id::text, r.peso_percentual
FROM regra_distribuicao_atividade r
JOIN progression_configuration_definition d ON d.legacy_atividade_config_id = r.atividade_config_id
JOIN progression_configuration_version v ON v.definition_id = d.id AND v.revision = 1;

INSERT INTO progression_configuration_version_distribution (id, configuration_version_id, attribute_key, weight)
SELECT snapshot_id, version_id, attribute_key, weight
FROM progression_configuration_distribution_backfill;

INSERT INTO progression_configuration_version_xp_rule (id, distribution_id, factor_key, multiplier, min_cutoff, max_cutoff)
SELECT gen_random_uuid(), m.snapshot_id, x.fator_calculo_id::text, x.peso_multiplicador, x.ponto_corte_min, x.ponto_corte_max
FROM regra_fator_xp x
JOIN progression_configuration_distribution_backfill m ON m.legacy_id = x.regra_distribuicao_atividade_id;

INSERT INTO progression_configuration_version_stress_rule (id, distribution_id, multiplier, min_cutoff, max_cutoff, type)
SELECT gen_random_uuid(), m.snapshot_id, s.peso_multiplicador, s.ponto_corte_min, s.ponto_corte_max, s.tipo
FROM regra_fator_estresse s
JOIN progression_configuration_distribution_backfill m ON m.legacy_id = s.regra_distribuicao_atividade_id;

INSERT INTO progression_configuration_version_factor (id, configuration_version_id, factor_key, tipo_input)
SELECT gen_random_uuid(), v.id, f.id::text, f.tipo_input
FROM progression_configuration_version v
CROSS JOIN fator_calculo f;

-- The current global skill rules become policy version 1. Historical records stay unlinked.
INSERT INTO progression_skill_policy (id, logical_key)
VALUES (gen_random_uuid(), 'global');

INSERT INTO progression_skill_policy_version (id, policy_id, revision)
SELECT gen_random_uuid(), id, 1 FROM progression_skill_policy WHERE logical_key = 'global';

UPDATE progression_skill_policy p
SET current_version_id = v.id
FROM progression_skill_policy_version v
WHERE v.policy_id = p.id AND v.revision = 1;

INSERT INTO progression_skill_policy_version_rule (id, policy_version_id, skill_key, attribute_key, distribution_weight)
SELECT gen_random_uuid(), v.id, r.habilidade_id::text, r.atributo_id::text, r.peso_distribuicao
FROM regra_distribuicao_habilidade r
JOIN progression_skill_policy p ON p.logical_key = 'global'
JOIN progression_skill_policy_version v ON v.policy_id = p.id AND v.revision = 1;
