-- TASK-012: durable identity and original response for external executions.
CREATE TABLE progression_external_execution (
    id UUID PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    subject_namespace VARCHAR(64) NOT NULL,
    subject_external_id VARCHAR(255) NOT NULL,
    configuration_key VARCHAR(255) NOT NULL,
    requested_revision INT,
    configuration_version_id UUID NOT NULL,
    skill_policy_version_id UUID NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_progression_external_execution_identity
        UNIQUE (source_system, idempotency_key),
    CONSTRAINT fk_progression_external_execution_configuration
        FOREIGN KEY (configuration_version_id) REFERENCES progression_configuration_version(id),
    CONSTRAINT fk_progression_external_execution_skill_policy
        FOREIGN KEY (skill_policy_version_id) REFERENCES progression_skill_policy_version(id)
);
