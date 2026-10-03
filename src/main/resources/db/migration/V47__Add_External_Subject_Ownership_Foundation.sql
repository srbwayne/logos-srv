ALTER TABLE progression_subject_identity
    ADD COLUMN identity_class VARCHAR(32),
    ADD COLUMN ownership_status VARCHAR(32),
    ADD COLUMN verification_status VARCHAR(32),
    ADD COLUMN ownership_version BIGINT;

UPDATE progression_subject_identity
SET identity_class = CASE WHEN namespace = 'logos-native' THEN 'LOGOS_NATIVE' ELSE 'EXTERNAL' END,
    ownership_status = 'ACTIVE',
    verification_status = CASE WHEN namespace = 'logos-native' THEN 'NOT_REQUIRED' ELSE 'UNVERIFIED' END,
    ownership_version = 0;

ALTER TABLE progression_subject_identity
    ALTER COLUMN identity_class SET NOT NULL,
    ALTER COLUMN ownership_status SET NOT NULL,
    ALTER COLUMN verification_status SET NOT NULL,
    ALTER COLUMN ownership_version SET NOT NULL,
    ALTER COLUMN ownership_version SET DEFAULT 0,
    ADD CONSTRAINT ck_progression_subject_identity_class
        CHECK (identity_class IN ('LOGOS_NATIVE', 'EXTERNAL')),
    ADD CONSTRAINT ck_progression_subject_identity_ownership_status
        CHECK (ownership_status IN ('ACTIVE', 'DISABLED', 'REVOKED')),
    ADD CONSTRAINT ck_progression_subject_identity_verification_status
        CHECK (verification_status IN ('NOT_REQUIRED', 'UNVERIFIED', 'VERIFIED', 'INVALIDATED')),
    ADD CONSTRAINT ck_progression_subject_identity_ownership_version
        CHECK (ownership_version >= 0),
    ADD CONSTRAINT ck_progression_subject_identity_class_matches_namespace
        CHECK (
            (namespace = 'logos-native' AND identity_class = 'LOGOS_NATIVE'
                AND verification_status = 'NOT_REQUIRED')
            OR (namespace <> 'logos-native' AND identity_class = 'EXTERNAL'
                AND verification_status <> 'NOT_REQUIRED')
        );

CREATE TABLE progression_subject_ownership_history (
    id UUID PRIMARY KEY,
    identity_id UUID NOT NULL,
    aggregate_version BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    previous_identity_class VARCHAR(32),
    new_identity_class VARCHAR(32) NOT NULL,
    previous_target_jogador_id UUID,
    new_target_jogador_id UUID NOT NULL,
    previous_ownership_status VARCHAR(32),
    new_ownership_status VARCHAR(32) NOT NULL,
    previous_verification_status VARCHAR(32),
    new_verification_status VARCHAR(32) NOT NULL,
    provenance VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32),
    actor_id VARCHAR(128),
    evidence_type VARCHAR(64),
    evidence_reference VARCHAR(255),
    reason VARCHAR(512),
    effective_at TIMESTAMPTZ,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT uq_progression_subject_ownership_history_version
        UNIQUE (identity_id, aggregate_version),
    CONSTRAINT ck_progression_subject_ownership_history_version
        CHECK (aggregate_version >= 0),
    CONSTRAINT ck_progression_subject_ownership_history_event_type
        CHECK (btrim(event_type) <> ''),
    CONSTRAINT ck_progression_subject_ownership_history_provenance
        CHECK (provenance IN (
            'LEGACY_LOGOS_NATIVE_UNKNOWN',
            'LEGACY_EXTERNAL_UNKNOWN',
            'LOGOS_NATIVE_REGISTRATION',
            'POC_SELF_LINK',
            'LOGOS_OPERATOR_ACTION'
        )),
    CONSTRAINT ck_progression_subject_ownership_history_actor_pair
        CHECK ((actor_type IS NULL) = (actor_id IS NULL)),
    CONSTRAINT fk_progression_subject_ownership_history_identity
        FOREIGN KEY (identity_id) REFERENCES progression_subject_identity(id) ON DELETE RESTRICT,
    CONSTRAINT fk_progression_subject_ownership_history_previous_target
        FOREIGN KEY (previous_target_jogador_id) REFERENCES jogador(id) ON DELETE RESTRICT,
    CONSTRAINT fk_progression_subject_ownership_history_new_target
        FOREIGN KEY (new_target_jogador_id) REFERENCES jogador(id) ON DELETE RESTRICT
);

CREATE INDEX ix_progression_subject_ownership_history_identity_recorded
    ON progression_subject_ownership_history (identity_id, recorded_at DESC, id DESC);

INSERT INTO progression_subject_ownership_history (
    id, identity_id, aggregate_version, event_type,
    previous_identity_class, new_identity_class,
    previous_target_jogador_id, new_target_jogador_id,
    previous_ownership_status, new_ownership_status,
    previous_verification_status, new_verification_status,
    provenance, actor_type, actor_id, evidence_type, evidence_reference,
    reason, effective_at
)
SELECT gen_random_uuid(),
       identity.id,
       identity.ownership_version,
       'INITIAL_CLASSIFICATION',
       NULL,
       identity.identity_class,
       NULL,
       identity.jogador_id,
       NULL,
       identity.ownership_status,
       NULL,
       identity.verification_status,
       CASE WHEN identity.identity_class = 'LOGOS_NATIVE'
           THEN 'LEGACY_LOGOS_NATIVE_UNKNOWN'
           ELSE 'LEGACY_EXTERNAL_UNKNOWN'
       END,
       NULL,
       NULL,
       NULL,
       NULL,
       NULL,
       NULL
FROM progression_subject_identity identity;

CREATE FUNCTION reject_progression_subject_ownership_history_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'progression subject ownership history is append-only';
END;
$$;

CREATE TRIGGER trg_progression_subject_ownership_history_no_update_delete
    BEFORE UPDATE OR DELETE ON progression_subject_ownership_history
    FOR EACH ROW EXECUTE FUNCTION reject_progression_subject_ownership_history_mutation();

CREATE TRIGGER trg_progression_subject_ownership_history_no_truncate
    BEFORE TRUNCATE ON progression_subject_ownership_history
    FOR EACH STATEMENT EXECUTE FUNCTION reject_progression_subject_ownership_history_mutation();
