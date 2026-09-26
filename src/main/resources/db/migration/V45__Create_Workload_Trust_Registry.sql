-- HARD-001 trust and replay persistence foundation.
-- Additive DDL only: no workload/key seed and no existing-row rewrite.

CREATE TABLE workload_principal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    principal_type VARCHAR(32) NOT NULL,
    principal_id VARCHAR(128) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    lifecycle_status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    disabled_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT ck_workload_principal_type
        CHECK (principal_type = 'WORKLOAD'),
    CONSTRAINT ck_workload_principal_id_not_blank
        CHECK (btrim(principal_id) <> ''),
    CONSTRAINT ck_workload_principal_issuer_not_blank
        CHECK (btrim(issuer) <> ''),
    CONSTRAINT uq_workload_principal_type_id
        UNIQUE (principal_type, principal_id),
    CONSTRAINT uq_workload_principal_issuer
        UNIQUE (issuer),
    CONSTRAINT ck_workload_principal_lifecycle_status
        CHECK (lifecycle_status IN ('ACTIVE', 'DISABLED', 'REVOKED')),
    CONSTRAINT ck_workload_principal_lifecycle_timestamps
        CHECK (
            (lifecycle_status = 'ACTIVE'
                AND disabled_at IS NULL
                AND revoked_at IS NULL)
            OR (lifecycle_status = 'DISABLED'
                AND disabled_at IS NOT NULL
                AND revoked_at IS NULL)
            OR (lifecycle_status = 'REVOKED'
                AND revoked_at IS NOT NULL)
        )
);

COMMENT ON COLUMN workload_principal.issuer IS
    'Stable HARD-001 trust lookup identity; treat as immutable after trust creation. No ON UPDATE CASCADE is configured.';

CREATE TABLE workload_signing_key (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workload_principal_id UUID NOT NULL,
    kid VARCHAR(64) NOT NULL,
    algorithm VARCHAR(16) NOT NULL,
    public_key_pem TEXT NOT NULL,
    public_key_spki_sha256 BYTEA NOT NULL,
    lifecycle_status VARCHAR(16) NOT NULL,
    not_before TIMESTAMPTZ NOT NULL,
    not_after TIMESTAMPTZ,
    activated_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    retired_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT fk_workload_signing_key_principal
        FOREIGN KEY (workload_principal_id)
        REFERENCES workload_principal (id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_workload_signing_key_id_principal
        UNIQUE (id, workload_principal_id),
    CONSTRAINT ck_workload_signing_key_kid_format
        CHECK (kid ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'),
    CONSTRAINT uq_workload_signing_key_principal_kid
        UNIQUE (workload_principal_id, kid),
    CONSTRAINT ck_workload_signing_key_algorithm
        CHECK (algorithm = 'ES256'),
    CONSTRAINT ck_workload_signing_key_public_pem_not_blank
        CHECK (btrim(public_key_pem) <> ''),
    CONSTRAINT ck_workload_signing_key_fingerprint_length
        CHECK (octet_length(public_key_spki_sha256) = 32),
    CONSTRAINT uq_workload_signing_key_public_fingerprint
        UNIQUE (public_key_spki_sha256),
    CONSTRAINT ck_workload_signing_key_lifecycle_status
        CHECK (lifecycle_status IN ('PENDING', 'ACTIVE', 'REVOKED', 'RETIRED')),
    CONSTRAINT ck_workload_signing_key_validity_window
        CHECK (not_after IS NULL OR not_after > not_before),
    CONSTRAINT ck_workload_signing_key_lifecycle_timestamps
        CHECK (
            (lifecycle_status = 'PENDING'
                AND activated_at IS NULL
                AND revoked_at IS NULL
                AND retired_at IS NULL)
            OR (lifecycle_status = 'ACTIVE'
                AND activated_at IS NOT NULL
                AND revoked_at IS NULL
                AND retired_at IS NULL)
            OR (lifecycle_status = 'REVOKED'
                AND revoked_at IS NOT NULL
                AND retired_at IS NULL)
            OR (lifecycle_status = 'RETIRED'
                AND activated_at IS NOT NULL
                AND retired_at IS NOT NULL)
        )
);

CREATE TABLE workload_trust_audit_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workload_principal_id UUID NOT NULL,
    credential_id UUID,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_workload_trust_audit_principal
        FOREIGN KEY (workload_principal_id)
        REFERENCES workload_principal (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_workload_trust_audit_credential
        FOREIGN KEY (credential_id, workload_principal_id)
        REFERENCES workload_signing_key (id, workload_principal_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_workload_trust_audit_actor_type
        CHECK (actor_type IN ('OPERATOR', 'SYSTEM')),
    CONSTRAINT ck_workload_trust_audit_actor_id_not_blank
        CHECK (btrim(actor_id) <> ''),
    CONSTRAINT ck_workload_trust_audit_action_not_blank
        CHECK (btrim(action) <> ''),
    CONSTRAINT ck_workload_trust_audit_reason_not_blank
        CHECK (btrim(reason) <> ''),
    CONSTRAINT ck_workload_trust_audit_metadata_object
        CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX ix_workload_trust_audit_principal_occurred
    ON workload_trust_audit_event (workload_principal_id, occurred_at DESC);

CREATE INDEX ix_workload_trust_audit_credential_occurred
    ON workload_trust_audit_event (credential_id, occurred_at DESC);

CREATE TABLE workload_assertion_replay (
    issuer VARCHAR(255) NOT NULL,
    jti UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT pk_workload_assertion_replay
        PRIMARY KEY (issuer, jti),
    CONSTRAINT fk_workload_assertion_replay_issuer
        FOREIGN KEY (issuer)
        REFERENCES workload_principal (issuer)
        ON DELETE RESTRICT
);

CREATE INDEX ix_workload_assertion_replay_expires_at
    ON workload_assertion_replay (expires_at);
