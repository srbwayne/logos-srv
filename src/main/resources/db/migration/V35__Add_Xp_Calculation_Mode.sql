-- TASK-014I: make XP calculation semantics explicit in immutable snapshots.
ALTER TABLE progression_configuration_version_xp_rule
    ADD COLUMN calculation_mode VARCHAR(32) NOT NULL DEFAULT 'FIXED';

ALTER TABLE progression_configuration_version_xp_rule
    ADD CONSTRAINT ck_progression_configuration_version_xp_rule_calculation_mode
    CHECK (calculation_mode IN ('FIXED', 'FACT_VALUE'));

-- The default is made explicit for every historical snapshot. No existing
-- configuration changes meaning: legacy rules are FIXED.
UPDATE progression_configuration_version_xp_rule
SET calculation_mode = 'FIXED'
WHERE calculation_mode IS NULL;
