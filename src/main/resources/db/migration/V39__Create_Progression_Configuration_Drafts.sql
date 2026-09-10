CREATE TABLE progression_configuration_draft (
    id UUID PRIMARY KEY,
    definition_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    base_xp INT,
    base_stress INT,
    CONSTRAINT fk_progression_configuration_draft_definition
        FOREIGN KEY (definition_id) REFERENCES progression_configuration_definition(id)
);

CREATE TABLE progression_configuration_draft_factor (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL,
    fator_calculo_id UUID NOT NULL,
    CONSTRAINT uq_progression_configuration_draft_factor
        UNIQUE (draft_id, fator_calculo_id),
    CONSTRAINT fk_progression_configuration_draft_factor_draft
        FOREIGN KEY (draft_id) REFERENCES progression_configuration_draft(id) ON DELETE CASCADE,
    CONSTRAINT fk_progression_configuration_draft_factor_definition
        FOREIGN KEY (fator_calculo_id) REFERENCES fator_calculo(id)
);

CREATE TABLE progression_configuration_draft_distribution (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL,
    attribute_id UUID NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_progression_configuration_draft_distribution_draft
        FOREIGN KEY (draft_id) REFERENCES progression_configuration_draft(id) ON DELETE CASCADE,
    CONSTRAINT fk_progression_configuration_draft_distribution_attribute
        FOREIGN KEY (attribute_id) REFERENCES atributo(id)
);

CREATE TABLE progression_configuration_draft_xp_rule (
    id UUID PRIMARY KEY,
    distribution_id UUID NOT NULL,
    fator_calculo_id UUID NOT NULL,
    multiplier DOUBLE PRECISION,
    min_cutoff DOUBLE PRECISION,
    max_cutoff DOUBLE PRECISION,
    calculation_mode VARCHAR(50),
    CONSTRAINT uq_progression_configuration_draft_xp_rule
        UNIQUE (distribution_id, fator_calculo_id),
    CONSTRAINT fk_progression_configuration_draft_xp_rule_distribution
        FOREIGN KEY (distribution_id) REFERENCES progression_configuration_draft_distribution(id) ON DELETE CASCADE,
    CONSTRAINT fk_progression_configuration_draft_xp_rule_factor
        FOREIGN KEY (fator_calculo_id) REFERENCES fator_calculo(id)
);

CREATE TABLE progression_configuration_draft_stress_rule (
    id UUID PRIMARY KEY,
    distribution_id UUID NOT NULL,
    multiplier DOUBLE PRECISION NOT NULL,
    min_cutoff DOUBLE PRECISION,
    max_cutoff DOUBLE PRECISION,
    type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_progression_configuration_draft_stress_rule_distribution
        FOREIGN KEY (distribution_id) REFERENCES progression_configuration_draft_distribution(id) ON DELETE CASCADE
);
