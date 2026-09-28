-- HARD-002/B1: empty relational authorization registry foundation.
-- No grants are seeded or derived from trust, progression, or subject records.

CREATE TABLE authorization_grant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    principal_type VARCHAR(32) NOT NULL,
    principal_id VARCHAR(128) NOT NULL,
    operation VARCHAR(40) NOT NULL,
    source VARCHAR(64),
    namespace VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT ck_authorization_grant_principal_type
        CHECK (principal_type = 'WORKLOAD'),
    CONSTRAINT ck_authorization_grant_principal_id
        CHECK (btrim(principal_id) <> '' AND principal_id = btrim(principal_id)),
    CONSTRAINT ck_authorization_grant_operation
        CHECK (operation IN (
            'PROGRESSION_EXECUTE',
            'PROGRESSION_EXECUTION_READ',
            'PROGRESSION_HISTORY_READ',
            'SUBJECT_PROVISION'
        )),
    CONSTRAINT ck_authorization_grant_source_grammar
        CHECK (source IS NULL OR source ~ '^[a-z0-9][a-z0-9._-]{0,63}$'),
    CONSTRAINT ck_authorization_grant_namespace_grammar
        CHECK (namespace IS NULL OR namespace ~ '^[a-z0-9][a-z0-9._-]{0,63}$'),
    CONSTRAINT ck_authorization_grant_applicability
        CHECK (
            (operation = 'PROGRESSION_EXECUTE' AND source IS NOT NULL AND namespace IS NOT NULL)
            OR (operation = 'PROGRESSION_EXECUTION_READ' AND source IS NOT NULL AND namespace IS NULL)
            OR (operation IN ('PROGRESSION_HISTORY_READ', 'SUBJECT_PROVISION')
                AND source IS NULL AND namespace IS NOT NULL)
        ),
    CONSTRAINT fk_authorization_grant_principal
        FOREIGN KEY (principal_type, principal_id)
        REFERENCES workload_principal (principal_type, principal_id)
        ON DELETE RESTRICT
        ON UPDATE NO ACTION,
    CONSTRAINT uq_authorization_grant_semantic
        UNIQUE NULLS NOT DISTINCT (principal_type, principal_id, operation, source, namespace)
);
