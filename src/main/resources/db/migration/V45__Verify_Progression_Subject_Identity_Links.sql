ALTER TABLE progression_subject_identity
    ADD COLUMN verification_status VARCHAR(32),
    ADD COLUMN verified_at TIMESTAMPTZ,
    ADD COLUMN verified_by_client_id VARCHAR(64);

UPDATE progression_subject_identity
SET verification_status = CASE
    WHEN namespace = 'logos-native' THEN 'LOGOS_NATIVE'
    ELSE 'UNVERIFIED'
END;

ALTER TABLE progression_subject_identity
    ALTER COLUMN verification_status SET NOT NULL,
    ALTER COLUMN verification_status SET DEFAULT 'UNVERIFIED',
    ADD CONSTRAINT ck_progression_subject_identity_verification_status
        CHECK (verification_status IN ('LOGOS_NATIVE', 'INTEGRATION_VERIFIED', 'UNVERIFIED'));

CREATE TABLE progression_subject_link_challenge (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    namespace VARCHAR(64) NOT NULL,
    jogador_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CONSTRAINT uq_progression_subject_link_challenge_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_progression_subject_link_challenge_namespace_not_blank CHECK (namespace <> ''),
    CONSTRAINT fk_progression_subject_link_challenge_jogador
        FOREIGN KEY (jogador_id) REFERENCES jogador(id) ON DELETE CASCADE
);

CREATE INDEX ix_progression_subject_link_challenge_jogador
    ON progression_subject_link_challenge(jogador_id, created_at DESC);
