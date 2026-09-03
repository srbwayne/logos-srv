-- TASK-009R: map namespaced external identities to existing Jogador subjects.
CREATE TABLE progression_subject_identity (
    id UUID PRIMARY KEY,
    namespace VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    jogador_id UUID NOT NULL,

    CONSTRAINT uk_progression_subject_identity_namespace_external_id
        UNIQUE (namespace, external_id),
    CONSTRAINT ck_progression_subject_identity_namespace_not_blank
        CHECK (namespace <> ''),
    CONSTRAINT ck_progression_subject_identity_external_id_not_blank
        CHECK (external_id <> ''),
    CONSTRAINT fk_progression_subject_identity_jogador
        FOREIGN KEY (jogador_id) REFERENCES jogador(id)
);

-- jogador.user_id is NOT NULL and references the unique app_user.id, so this
-- produces exactly one native mapping for every existing player.
INSERT INTO progression_subject_identity (id, namespace, external_id, jogador_id)
SELECT gen_random_uuid(), 'logos-native', CAST(u.id AS VARCHAR(255)), j.id
FROM jogador j
JOIN app_user u ON u.id = j.user_id;
