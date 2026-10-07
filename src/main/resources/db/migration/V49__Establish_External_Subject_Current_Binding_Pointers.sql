-- Establish a database-owned current binding for every known external locator.
-- This migration is intentionally forward-only once successor rows exist.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM progression_subject_identity
        WHERE namespace IS NULL OR btrim(namespace) = ''
           OR external_id IS NULL OR btrim(external_id) = ''
           OR jogador_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V49 preflight failed: invalid subject identity locator or target';
    END IF;

    IF EXISTS (
        SELECT 1 FROM progression_subject_ownership_history history
        LEFT JOIN progression_subject_identity identity ON identity.id = history.identity_id
        WHERE identity.id IS NULL
    ) THEN
        RAISE EXCEPTION 'V49 preflight failed: orphan subject ownership history';
    END IF;

    IF EXISTS (
        SELECT 1 FROM progression_subject_ownership_history
        GROUP BY identity_id, aggregate_version HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'V49 preflight failed: duplicate identity history versions';
    END IF;
END;
$$;

ALTER TABLE progression_subject_identity
    ADD CONSTRAINT uq_progression_subject_identity_id_locator
        UNIQUE (id, namespace, external_id);

CREATE INDEX ix_progression_subject_identity_locator_history
    ON progression_subject_identity (namespace, external_id, id);

CREATE TABLE progression_subject_current_binding (
    namespace VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    current_identity_id UUID NOT NULL,
    CONSTRAINT pk_progression_subject_current_binding PRIMARY KEY (namespace, external_id),
    CONSTRAINT uq_progression_subject_current_binding_identity UNIQUE (current_identity_id),
    CONSTRAINT ck_progression_subject_current_binding_namespace_not_blank CHECK (btrim(namespace) <> ''),
    CONSTRAINT ck_progression_subject_current_binding_external_id_not_blank CHECK (btrim(external_id) <> ''),
    CONSTRAINT fk_progression_subject_current_binding_identity_locator
        FOREIGN KEY (current_identity_id, namespace, external_id)
        REFERENCES progression_subject_identity (id, namespace, external_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED
);

INSERT INTO progression_subject_current_binding (namespace, external_id, current_identity_id)
SELECT namespace, external_id, id
FROM progression_subject_identity
ORDER BY namespace, external_id, id;

ALTER TABLE progression_subject_identity
    ADD CONSTRAINT fk_progression_subject_identity_current_locator
        FOREIGN KEY (namespace, external_id)
        REFERENCES progression_subject_current_binding (namespace, external_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE progression_subject_identity
    ADD COLUMN predecessor_identity_id UUID,
    ADD CONSTRAINT ck_progression_subject_identity_predecessor_not_self
        CHECK (predecessor_identity_id IS NULL OR predecessor_identity_id <> id),
    ADD CONSTRAINT uq_progression_subject_identity_predecessor
        UNIQUE (predecessor_identity_id),
    ADD CONSTRAINT fk_progression_subject_identity_predecessor_locator
        FOREIGN KEY (predecessor_identity_id, namespace, external_id)
        REFERENCES progression_subject_identity (id, namespace, external_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;

CREATE INDEX ix_progression_subject_identity_predecessor
    ON progression_subject_identity (predecessor_identity_id)
    WHERE predecessor_identity_id IS NOT NULL;

CREATE TABLE progression_subject_reassignment_authorization (
    authorization_id UUID PRIMARY KEY,
    reassignment_request_id UUID NOT NULL UNIQUE,
    namespace VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    predecessor_identity_id UUID NOT NULL,
    predecessor_ownership_version BIGINT NOT NULL,
    predecessor_target_jogador_id UUID NOT NULL,
    proposed_successor_target_jogador_id UUID NOT NULL,
    recovery_basis VARCHAR(2048) NOT NULL,
    reviewed_case_reference VARCHAR(255) NOT NULL,
    reviewer_principal_id VARCHAR(128) NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT ck_progression_subject_reassignment_authorization_locator CHECK (
        btrim(namespace) <> '' AND btrim(external_id) <> ''
    ),
    CONSTRAINT ck_progression_subject_reassignment_authorization_version
        CHECK (predecessor_ownership_version >= 0),
    CONSTRAINT ck_progression_subject_reassignment_authorization_targets
        CHECK (predecessor_target_jogador_id <> proposed_successor_target_jogador_id),
    CONSTRAINT ck_progression_subject_reassignment_authorization_text CHECK (
        btrim(recovery_basis) <> '' AND btrim(reviewed_case_reference) <> ''
            AND btrim(reviewer_principal_id) <> ''
    ),
    CONSTRAINT fk_progression_subject_reassignment_authorization_predecessor
        FOREIGN KEY (predecessor_identity_id, namespace, external_id)
        REFERENCES progression_subject_identity (id, namespace, external_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_progression_subject_reassignment_authorization_old_target
        FOREIGN KEY (predecessor_target_jogador_id) REFERENCES jogador (id) ON DELETE RESTRICT,
    CONSTRAINT fk_progression_subject_reassignment_authorization_new_target
        FOREIGN KEY (proposed_successor_target_jogador_id) REFERENCES jogador (id) ON DELETE RESTRICT
);

CREATE FUNCTION reject_progression_subject_reassignment_authorization_mutation()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'progression subject reassignment authorization is immutable';
END;
$$;

CREATE TRIGGER trg_progression_subject_reassignment_authorization_immutable
    BEFORE UPDATE OR DELETE ON progression_subject_reassignment_authorization
    FOR EACH ROW EXECUTE FUNCTION reject_progression_subject_reassignment_authorization_mutation();

CREATE TRIGGER trg_progression_subject_reassignment_authorization_no_truncate
    BEFORE TRUNCATE ON progression_subject_reassignment_authorization
    FOR EACH STATEMENT EXECUTE FUNCTION reject_progression_subject_reassignment_authorization_mutation();

ALTER TABLE progression_subject_ownership_history
    ADD COLUMN predecessor_identity_id UUID,
    ADD COLUMN predecessor_ownership_version BIGINT,
    ADD COLUMN reassignment_request_id UUID,
    ADD COLUMN reassignment_authorization_id UUID,
    ADD CONSTRAINT ck_progression_subject_ownership_history_reassignment CHECK (
        event_type <> 'OWNERSHIP_REASSIGNED'
        OR (
            predecessor_identity_id IS NOT NULL
            AND predecessor_ownership_version IS NOT NULL
            AND predecessor_ownership_version >= 0
            AND reassignment_request_id IS NOT NULL
            AND reassignment_authorization_id IS NOT NULL
            AND previous_identity_class = 'EXTERNAL'
            AND new_identity_class = 'EXTERNAL'
            AND previous_ownership_status = 'REVOKED'
            AND new_ownership_status = 'DISABLED'
            AND previous_target_jogador_id IS NOT NULL
            AND previous_target_jogador_id <> new_target_jogador_id
            AND previous_verification_status IN ('UNVERIFIED', 'VERIFIED', 'INVALIDATED')
            AND new_verification_status = CASE previous_verification_status
                WHEN 'VERIFIED' THEN 'UNVERIFIED'
                ELSE previous_verification_status
            END
            AND provenance = 'LOGOS_OPERATOR_ACTION'
            AND actor_type = 'WORKLOAD_OPERATOR'
            AND actor_id IS NOT NULL AND btrim(actor_id) <> ''
            AND evidence_type = 'ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION'
            AND evidence_reference IS NOT NULL AND btrim(evidence_reference) <> ''
            AND reason IS NOT NULL AND btrim(reason) <> ''
        )
    ),
    ADD CONSTRAINT fk_progression_subject_ownership_history_predecessor
        FOREIGN KEY (predecessor_identity_id) REFERENCES progression_subject_identity (id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_psoh_reassignment_authorization
        FOREIGN KEY (reassignment_authorization_id)
        REFERENCES progression_subject_reassignment_authorization (authorization_id)
        ON DELETE RESTRICT;

CREATE UNIQUE INDEX uq_progression_subject_ownership_history_reassignment_request
    ON progression_subject_ownership_history (reassignment_request_id)
    WHERE event_type = 'OWNERSHIP_REASSIGNED';

CREATE UNIQUE INDEX uq_psoh_reassignment_authorization
    ON progression_subject_ownership_history (reassignment_authorization_id)
    WHERE event_type = 'OWNERSHIP_REASSIGNED';

CREATE FUNCTION validate_progression_subject_reassignment_history()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    successor progression_subject_identity%ROWTYPE;
    predecessor progression_subject_identity%ROWTYPE;
    approval_row progression_subject_reassignment_authorization%ROWTYPE;
    selected_identity UUID;
BEGIN
    IF NEW.event_type <> 'OWNERSHIP_REASSIGNED' THEN
        RETURN NULL;
    END IF;

    SELECT * INTO successor FROM progression_subject_identity WHERE id = NEW.identity_id;
    SELECT * INTO predecessor FROM progression_subject_identity WHERE id = NEW.predecessor_identity_id;
    SELECT * INTO approval_row
    FROM progression_subject_reassignment_authorization
    WHERE authorization_id = NEW.reassignment_authorization_id;
    SELECT current_identity_id INTO selected_identity
    FROM progression_subject_current_binding
    WHERE namespace = successor.namespace AND external_id = successor.external_id;

    IF successor.id IS NULL OR predecessor.id IS NULL OR approval_row.authorization_id IS NULL
       OR successor.predecessor_identity_id IS DISTINCT FROM predecessor.id
       OR successor.namespace IS DISTINCT FROM predecessor.namespace
       OR successor.external_id IS DISTINCT FROM predecessor.external_id
       OR successor.identity_class <> 'EXTERNAL'
       OR successor.ownership_status <> 'DISABLED'
       OR successor.ownership_version <> 0
       OR successor.jogador_id <> NEW.new_target_jogador_id
       OR predecessor.identity_class <> 'EXTERNAL'
       OR predecessor.ownership_status <> 'REVOKED'
       OR predecessor.ownership_version <> NEW.predecessor_ownership_version
       OR predecessor.jogador_id <> NEW.previous_target_jogador_id
       OR predecessor.verification_status <> NEW.previous_verification_status
       OR successor.verification_status <> NEW.new_verification_status
       OR selected_identity IS DISTINCT FROM successor.id
       OR approval_row.reassignment_request_id <> NEW.reassignment_request_id
       OR approval_row.namespace <> successor.namespace
       OR approval_row.external_id <> successor.external_id
       OR approval_row.predecessor_identity_id <> predecessor.id
       OR approval_row.predecessor_ownership_version <> predecessor.ownership_version
       OR approval_row.predecessor_target_jogador_id <> predecessor.jogador_id
       OR approval_row.proposed_successor_target_jogador_id <> successor.jogador_id
       OR approval_row.authorization_id::text <> NEW.evidence_reference
       OR approval_row.reviewer_principal_id = NEW.actor_id THEN
        RAISE EXCEPTION 'OWNERSHIP_REASSIGNED history must match its successor, predecessor, pointer, and authorization';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_progression_subject_reassignment_history_integrity
    AFTER INSERT ON progression_subject_ownership_history
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION validate_progression_subject_reassignment_history();

CREATE FUNCTION validate_progression_subject_current_binding_identity()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    selected_identity UUID;
    lineage_cycle BOOLEAN;
BEGIN
    SELECT current_identity_id INTO selected_identity
    FROM progression_subject_current_binding
    WHERE namespace = NEW.namespace AND external_id = NEW.external_id;

    IF selected_identity IS DISTINCT FROM NEW.id THEN
        RAISE EXCEPTION 'new subject identity must be selected by its current locator pointer';
    END IF;

    IF NEW.predecessor_identity_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM progression_subject_identity predecessor
            JOIN progression_subject_ownership_history event
              ON event.identity_id = NEW.id
             AND event.predecessor_identity_id = predecessor.id
             AND event.event_type = 'OWNERSHIP_REASSIGNED'
            JOIN progression_subject_reassignment_authorization approval_row
              ON approval_row.authorization_id = event.reassignment_authorization_id
            WHERE predecessor.id = NEW.predecessor_identity_id
              AND predecessor.namespace = NEW.namespace
              AND predecessor.external_id = NEW.external_id
              AND predecessor.ownership_status = 'REVOKED'
              AND predecessor.ownership_version = event.predecessor_ownership_version
              AND predecessor.jogador_id = event.previous_target_jogador_id
              AND NEW.jogador_id = event.new_target_jogador_id
              AND approval_row.reassignment_request_id = event.reassignment_request_id
              AND approval_row.predecessor_identity_id = predecessor.id
              AND approval_row.predecessor_ownership_version = predecessor.ownership_version
              AND approval_row.predecessor_target_jogador_id = predecessor.jogador_id
              AND approval_row.proposed_successor_target_jogador_id = NEW.jogador_id
              AND approval_row.namespace = NEW.namespace
              AND approval_row.external_id = NEW.external_id
              AND approval_row.reviewer_principal_id <> event.actor_id
        ) THEN
            RAISE EXCEPTION 'successor identity requires an exact reassignment history and authorization';
        END IF;
    END IF;

    WITH RECURSIVE lineage(identity_id, predecessor_id, path, cycle) AS (
        SELECT identity.id, identity.predecessor_identity_id,
               ARRAY[identity.id]::UUID[], FALSE
        FROM progression_subject_identity identity WHERE identity.id = NEW.id
        UNION ALL
        SELECT predecessor.id, predecessor.predecessor_identity_id,
               lineage.path || predecessor.id,
               predecessor.id = ANY(lineage.path)
        FROM lineage
        JOIN progression_subject_identity predecessor ON predecessor.id = lineage.predecessor_id
        WHERE lineage.predecessor_id IS NOT NULL AND NOT lineage.cycle
    )
    SELECT COALESCE(bool_or(cycle), FALSE) INTO lineage_cycle FROM lineage;

    IF lineage_cycle THEN
        RAISE EXCEPTION 'subject identity lineage must be acyclic';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_progression_subject_current_binding_identity
    AFTER INSERT ON progression_subject_identity
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION validate_progression_subject_current_binding_identity();

CREATE FUNCTION validate_progression_subject_current_binding_change()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.current_identity_id IS DISTINCT FROM NEW.current_identity_id
       AND NOT EXISTS (
           SELECT 1
           FROM progression_subject_identity successor
           JOIN progression_subject_ownership_history event
             ON event.identity_id = successor.id
            AND event.event_type = 'OWNERSHIP_REASSIGNED'
            AND event.predecessor_identity_id = OLD.current_identity_id
           WHERE successor.id = NEW.current_identity_id
             AND successor.predecessor_identity_id = OLD.current_identity_id
             AND successor.namespace = NEW.namespace
             AND successor.external_id = NEW.external_id
       ) THEN
        RAISE EXCEPTION 'current binding pointer can move only to its audited successor';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_progression_subject_current_binding_update_guard
    AFTER UPDATE ON progression_subject_current_binding
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION validate_progression_subject_current_binding_change();

CREATE FUNCTION reject_progression_subject_current_binding_delete()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'current subject binding pointers cannot be deleted';
END;
$$;

CREATE TRIGGER trg_progression_subject_current_binding_delete_guard
    BEFORE DELETE ON progression_subject_current_binding
    FOR EACH ROW EXECUTE FUNCTION reject_progression_subject_current_binding_delete();

CREATE FUNCTION reject_progression_subject_current_binding_truncate()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'current subject binding pointers cannot be truncated';
END;
$$;

CREATE TRIGGER trg_progression_subject_current_binding_truncate_guard
    BEFORE TRUNCATE ON progression_subject_current_binding
    FOR EACH STATEMENT EXECUTE FUNCTION reject_progression_subject_current_binding_truncate();

CREATE FUNCTION create_initial_progression_subject_current_binding()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.predecessor_identity_id IS NULL THEN
        INSERT INTO progression_subject_current_binding (namespace, external_id, current_identity_id)
        VALUES (NEW.namespace, NEW.external_id, NEW.id)
        ON CONFLICT (namespace, external_id) DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_progression_subject_identity_initial_pointer
    AFTER INSERT ON progression_subject_identity
    FOR EACH ROW EXECUTE FUNCTION create_initial_progression_subject_current_binding();

CREATE FUNCTION reject_progression_subject_identity_locator_mutation()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.id IS DISTINCT FROM NEW.id
       OR OLD.namespace IS DISTINCT FROM NEW.namespace
       OR OLD.external_id IS DISTINCT FROM NEW.external_id
       OR OLD.predecessor_identity_id IS DISTINCT FROM NEW.predecessor_identity_id THEN
        RAISE EXCEPTION 'subject identity identity, locator, and lineage are immutable';
    END IF;
    IF OLD.ownership_status = 'REVOKED'
       AND (OLD.jogador_id IS DISTINCT FROM NEW.jogador_id
            OR OLD.ownership_status IS DISTINCT FROM NEW.ownership_status
            OR OLD.ownership_version IS DISTINCT FROM NEW.ownership_version) THEN
        RAISE EXCEPTION 'revoked subject binding is terminal and immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_progression_subject_identity_immutable_binding_fields
    BEFORE UPDATE ON progression_subject_identity
    FOR EACH ROW EXECUTE FUNCTION reject_progression_subject_identity_locator_mutation();

-- This is intentionally the last relaxation: all pointers and their reciprocal
-- locator coverage are installed and validated before historical duplicates
-- become physically representable.
DO $$
BEGIN
    IF (SELECT count(*) FROM progression_subject_identity)
       <> (SELECT count(*) FROM progression_subject_current_binding) THEN
        RAISE EXCEPTION 'V49 pointer backfill count does not match identity count';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM progression_subject_identity identity
        LEFT JOIN progression_subject_current_binding pointer
          ON pointer.namespace = identity.namespace AND pointer.external_id = identity.external_id
        WHERE pointer.namespace IS NULL
    ) THEN
        RAISE EXCEPTION 'V49 pointer backfill is missing an identity locator';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM progression_subject_current_binding pointer
        LEFT JOIN progression_subject_identity identity
          ON identity.id = pointer.current_identity_id
         AND identity.namespace = pointer.namespace
         AND identity.external_id = pointer.external_id
        WHERE identity.id IS NULL
    ) THEN
        RAISE EXCEPTION 'V49 pointer locator does not match its identity';
    END IF;
END;
$$;

ALTER TABLE progression_subject_identity
    DROP CONSTRAINT uk_progression_subject_identity_namespace_external_id;
