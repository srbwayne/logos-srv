ALTER TABLE progression_external_execution
    ADD COLUMN request_json TEXT,
    ADD COLUMN processing_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 1,
    ADD COLUMN last_error TEXT;

UPDATE progression_external_execution
SET request_json = '{}'
WHERE request_json IS NULL;

ALTER TABLE progression_external_execution
    ALTER COLUMN request_json SET NOT NULL;

CREATE INDEX ix_progression_external_execution_processing_status
    ON progression_external_execution (processing_status, created_at);
