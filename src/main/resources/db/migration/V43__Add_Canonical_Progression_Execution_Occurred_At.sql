ALTER TABLE progression_external_execution
    ADD COLUMN occurred_at TIMESTAMPTZ;

ALTER TABLE progression_external_execution
    ALTER COLUMN occurred_at SET DEFAULT CURRENT_TIMESTAMP;
