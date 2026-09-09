ALTER TABLE fator_calculo
    ADD COLUMN semantic_key VARCHAR(64);

CREATE UNIQUE INDEX uq_fator_calculo_semantic_key
    ON fator_calculo (semantic_key)
    WHERE semantic_key IS NOT NULL;

ALTER TABLE progression_configuration_version_xp_rule
    ALTER COLUMN factor_key TYPE VARCHAR(64);

ALTER TABLE progression_configuration_version_factor
    ALTER COLUMN factor_key TYPE VARCHAR(64);

ALTER TABLE progression_configuration_version
    ADD COLUMN fact_key_generation VARCHAR(16) NOT NULL DEFAULT 'LEGACY_UUID';

ALTER TABLE progression_configuration_version
    ADD CONSTRAINT ck_progression_configuration_version_fact_key_generation
    CHECK (fact_key_generation IN ('LEGACY_UUID', 'SEMANTIC'));
