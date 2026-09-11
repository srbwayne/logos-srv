ALTER TABLE progression_configuration_version
    ADD COLUMN source_draft_version BIGINT;

CREATE UNIQUE INDEX uq_progression_configuration_version_source_draft
    ON progression_configuration_version (definition_id, source_draft_version)
    WHERE source_draft_version IS NOT NULL;
