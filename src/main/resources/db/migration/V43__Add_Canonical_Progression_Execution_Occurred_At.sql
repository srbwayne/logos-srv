ALTER TABLE progression_external_execution
    ADD COLUMN occurred_at TIMESTAMPTZ;

ALTER TABLE progression_external_execution
    ALTER COLUMN occurred_at SET DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX ix_progression_external_execution_subject_history
    ON progression_external_execution (
        subject_namespace,
        subject_external_id,
        occurred_at DESC NULLS LAST,
        created_at DESC,
        id DESC
    );
