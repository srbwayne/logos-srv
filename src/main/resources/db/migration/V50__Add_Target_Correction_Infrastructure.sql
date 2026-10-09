-- Add database-only infrastructure for reviewed target-correction successors.
-- The migration is transactional and refuses malformed V49 state; it does not
-- rewrite history, identities, or current pointers.

DO $$
DECLARE
    invalid_reassignment BOOLEAN;
    invalid_lineage BOOLEAN;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM progression_subject_identity identity
        LEFT JOIN progression_subject_current_binding pointer
          ON pointer.namespace = identity.namespace
         AND pointer.external_id = identity.external_id
        WHERE pointer.namespace IS NULL
    ) OR EXISTS (
        SELECT 1
        FROM progression_subject_current_binding pointer
        LEFT JOIN progression_subject_identity identity
          ON identity.id = pointer.current_identity_id
         AND identity.namespace = pointer.namespace
         AND identity.external_id = pointer.external_id
        WHERE identity.id IS NULL
    ) THEN
        RAISE EXCEPTION 'V50 preflight failed: current pointer coverage or locator is invalid';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM progression_subject_ownership_history history
        LEFT JOIN progression_subject_identity identity ON identity.id = history.identity_id
        WHERE identity.id IS NULL
    ) OR EXISTS (
        SELECT 1
        FROM progression_subject_ownership_history
        GROUP BY identity_id, aggregate_version
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'V50 preflight failed: ownership history is orphaned or duplicated';
    END IF;

    WITH RECURSIVE lineage(identity_id, predecessor_id, namespace, external_id, path, cycle, locator_mismatch) AS (
        SELECT identity.id, identity.predecessor_identity_id, identity.namespace, identity.external_id,
               ARRAY[identity.id]::UUID[], FALSE, FALSE
        FROM progression_subject_identity identity
        UNION ALL
        SELECT predecessor.id, predecessor.predecessor_identity_id, lineage.namespace, lineage.external_id,
               lineage.path || predecessor.id, predecessor.id = ANY(lineage.path),
               predecessor.namespace IS DISTINCT FROM lineage.namespace
                   OR predecessor.external_id IS DISTINCT FROM lineage.external_id
        FROM lineage
        JOIN progression_subject_identity predecessor ON predecessor.id = lineage.predecessor_id
        WHERE lineage.predecessor_id IS NOT NULL AND NOT lineage.cycle
    )
    SELECT COALESCE(bool_or(cycle OR locator_mismatch), FALSE)
      INTO invalid_lineage
    FROM lineage;

    IF invalid_lineage OR EXISTS (
        SELECT 1
        FROM progression_subject_identity child
        LEFT JOIN progression_subject_identity predecessor ON predecessor.id = child.predecessor_identity_id
        WHERE child.predecessor_identity_id IS NOT NULL
          AND (predecessor.id IS NULL
               OR child.id = predecessor.id
               OR child.namespace IS DISTINCT FROM predecessor.namespace
               OR child.external_id IS DISTINCT FROM predecessor.external_id)
    ) THEN
        RAISE EXCEPTION 'V50 preflight failed: identity lineage is invalid';
    END IF;

    -- Re-evaluate historical reassignment snapshots against their immutable
    -- authorization and lineage. Successor lifecycle fields and pointer
    -- membership may legitimately change after reassignment creation.
    SELECT EXISTS (
        SELECT 1
        FROM progression_subject_ownership_history event
        LEFT JOIN progression_subject_identity successor ON successor.id = event.identity_id
        LEFT JOIN progression_subject_identity predecessor ON predecessor.id = event.predecessor_identity_id
        LEFT JOIN progression_subject_reassignment_authorization approval
          ON approval.authorization_id = event.reassignment_authorization_id
        WHERE event.event_type = 'OWNERSHIP_REASSIGNED'
          AND (successor.id IS NULL OR predecessor.id IS NULL OR approval.authorization_id IS NULL
               OR event.aggregate_version IS DISTINCT FROM 0
               OR event.previous_identity_class IS DISTINCT FROM 'EXTERNAL'
               OR event.new_identity_class IS DISTINCT FROM 'EXTERNAL'
               OR event.previous_ownership_status IS DISTINCT FROM 'REVOKED'
               OR event.new_ownership_status IS DISTINCT FROM 'DISABLED'
               OR event.previous_verification_status IS DISTINCT FROM predecessor.verification_status
               OR event.previous_target_jogador_id IS DISTINCT FROM predecessor.jogador_id
               OR event.provenance IS DISTINCT FROM 'LOGOS_OPERATOR_ACTION'
               OR event.actor_type IS DISTINCT FROM 'WORKLOAD_OPERATOR'
               OR event.actor_id IS NULL OR btrim(event.actor_id) = ''
               OR event.evidence_type IS DISTINCT FROM 'ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION'
               OR event.evidence_reference IS DISTINCT FROM event.reassignment_authorization_id::text
               OR event.reason IS NULL OR btrim(event.reason) = ''
               OR successor.predecessor_identity_id IS DISTINCT FROM predecessor.id
               OR successor.namespace IS DISTINCT FROM predecessor.namespace
               OR successor.external_id IS DISTINCT FROM predecessor.external_id
               OR successor.identity_class IS DISTINCT FROM 'EXTERNAL'
               OR predecessor.identity_class IS DISTINCT FROM 'EXTERNAL'
               OR predecessor.ownership_status IS DISTINCT FROM 'REVOKED'
               OR predecessor.ownership_version IS DISTINCT FROM event.predecessor_ownership_version
               OR predecessor.jogador_id IS DISTINCT FROM event.previous_target_jogador_id
               OR approval.reassignment_request_id IS DISTINCT FROM event.reassignment_request_id
               OR approval.namespace IS DISTINCT FROM successor.namespace
               OR approval.external_id IS DISTINCT FROM successor.external_id
               OR approval.predecessor_identity_id IS DISTINCT FROM event.predecessor_identity_id
               OR approval.predecessor_ownership_version IS DISTINCT FROM event.predecessor_ownership_version
               OR approval.predecessor_target_jogador_id IS DISTINCT FROM event.previous_target_jogador_id
               OR approval.proposed_successor_target_jogador_id IS DISTINCT FROM event.new_target_jogador_id
               OR approval.authorization_id::text IS DISTINCT FROM event.evidence_reference
               OR approval.reviewer_principal_id IS NOT DISTINCT FROM event.actor_id)
    ) INTO invalid_reassignment;

    IF invalid_reassignment OR EXISTS (
        SELECT 1
        FROM progression_subject_identity successor
        LEFT JOIN progression_subject_ownership_history reassignment
          ON reassignment.identity_id = successor.id
         AND reassignment.event_type = 'OWNERSHIP_REASSIGNED'
         AND reassignment.aggregate_version = 0
        WHERE successor.predecessor_identity_id IS NOT NULL
          AND reassignment.id IS NULL
    ) THEN
        RAISE EXCEPTION 'V50 preflight failed: existing reassignment lineage is invalid';
    END IF;
END;
$$;

CREATE TABLE progression_subject_target_correction_authorization (
    authorization_id UUID PRIMARY KEY,
    correction_request_id UUID NOT NULL UNIQUE,
    namespace VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    predecessor_identity_id UUID NOT NULL,
    predecessor_ownership_version BIGINT NOT NULL,
    predecessor_target_jogador_id UUID NOT NULL,
    corrected_target_jogador_id UUID NOT NULL,
    correction_basis VARCHAR(2048) NOT NULL,
    source_assertion_reference VARCHAR(512) NOT NULL,
    authoritative_fact_reference VARCHAR(512) NOT NULL,
    reviewed_case_reference VARCHAR(255) NOT NULL,
    reviewer_principal_id VARCHAR(128) NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT uq_tca_auth_request
        UNIQUE (authorization_id, correction_request_id),
    CONSTRAINT ck_progression_subject_target_correction_authorization_locator
        CHECK (btrim(namespace) <> '' AND btrim(external_id) <> ''),
    CONSTRAINT ck_progression_subject_target_correction_authorization_version
        CHECK (predecessor_ownership_version >= 0),
    CONSTRAINT ck_progression_subject_target_correction_authorization_targets
        CHECK (predecessor_target_jogador_id <> corrected_target_jogador_id),
    CONSTRAINT ck_progression_subject_target_correction_authorization_text
        CHECK (btrim(correction_basis) <> ''
           AND btrim(source_assertion_reference) <> ''
           AND btrim(authoritative_fact_reference) <> ''
           AND btrim(reviewed_case_reference) <> ''
           AND btrim(reviewer_principal_id) <> ''),
    CONSTRAINT fk_tca_predecessor_locator
        FOREIGN KEY (predecessor_identity_id, namespace, external_id)
        REFERENCES progression_subject_identity (id, namespace, external_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_tca_old_target
        FOREIGN KEY (predecessor_target_jogador_id) REFERENCES jogador (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_tca_new_target
        FOREIGN KEY (corrected_target_jogador_id) REFERENCES jogador (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE FUNCTION reject_target_correction_authorization_mutation()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'progression subject target correction authorization is immutable';
END;
$$;

CREATE TRIGGER trg_tca_immutable
    BEFORE UPDATE OR DELETE ON progression_subject_target_correction_authorization
    FOR EACH ROW EXECUTE FUNCTION reject_target_correction_authorization_mutation();

CREATE TRIGGER trg_tca_no_truncate
    BEFORE TRUNCATE ON progression_subject_target_correction_authorization
    FOR EACH STATEMENT EXECUTE FUNCTION reject_target_correction_authorization_mutation();

ALTER TABLE progression_subject_ownership_history
    ADD COLUMN correction_request_id UUID,
    ADD COLUMN target_correction_authorization_id UUID,
    ADD CONSTRAINT ck_progression_subject_ownership_history_target_correction CHECK (
        (event_type = 'OWNERSHIP_TARGET_CORRECTED' AND
            correction_request_id IS NOT NULL AND
            target_correction_authorization_id IS NOT NULL AND
            predecessor_identity_id IS NOT NULL AND
            predecessor_ownership_version IS NOT NULL AND
            predecessor_ownership_version >= 0 AND
            aggregate_version = 0 AND
            previous_identity_class IS NOT DISTINCT FROM 'EXTERNAL' AND
            new_identity_class IS NOT DISTINCT FROM 'EXTERNAL' AND
            previous_ownership_status IS NOT NULL AND
            previous_ownership_status IN ('ACTIVE', 'DISABLED') AND
            new_ownership_status IS NOT DISTINCT FROM 'DISABLED' AND
            previous_target_jogador_id IS NOT NULL AND
            new_target_jogador_id IS NOT NULL AND
            previous_target_jogador_id IS DISTINCT FROM new_target_jogador_id AND
            previous_verification_status IS NOT NULL AND
            previous_verification_status IN ('UNVERIFIED', 'VERIFIED', 'INVALIDATED') AND
            new_verification_status IS NOT DISTINCT FROM CASE previous_verification_status
                WHEN 'VERIFIED' THEN 'UNVERIFIED'
                WHEN 'UNVERIFIED' THEN 'UNVERIFIED'
                WHEN 'INVALIDATED' THEN 'INVALIDATED'
                ELSE NULL
            END AND
            provenance IS NOT DISTINCT FROM 'LOGOS_OPERATOR_ACTION' AND
            actor_type IS NOT DISTINCT FROM 'WORKLOAD_OPERATOR' AND
            actor_id IS NOT NULL AND btrim(actor_id) <> '' AND
            evidence_type IS NOT DISTINCT FROM 'ADMINISTRATIVE_TARGET_CORRECTION_AUTHORIZATION' AND
            evidence_reference IS NOT DISTINCT FROM target_correction_authorization_id::text AND
            reason IS NOT NULL AND btrim(reason) <> '') OR
        (event_type <> 'OWNERSHIP_TARGET_CORRECTED' AND
            correction_request_id IS NULL AND target_correction_authorization_id IS NULL)
    ),
    ADD CONSTRAINT fk_psoh_target_correction_authorization
        FOREIGN KEY (target_correction_authorization_id)
        REFERENCES progression_subject_target_correction_authorization (authorization_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    ADD CONSTRAINT fk_psoh_target_correction_correlation
        FOREIGN KEY (target_correction_authorization_id, correction_request_id)
        REFERENCES progression_subject_target_correction_authorization
            (authorization_id, correction_request_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE UNIQUE INDEX uq_psoh_target_correction_request
    ON progression_subject_ownership_history (correction_request_id)
    WHERE event_type = 'OWNERSHIP_TARGET_CORRECTED';

CREATE UNIQUE INDEX uq_psoh_target_correction_authorization
    ON progression_subject_ownership_history (target_correction_authorization_id)
    WHERE event_type = 'OWNERSHIP_TARGET_CORRECTED';

CREATE OR REPLACE FUNCTION validate_progression_subject_reassignment_history()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    successor progression_subject_identity%ROWTYPE;
    predecessor progression_subject_identity%ROWTYPE;
    approval_row progression_subject_reassignment_authorization%ROWTYPE;
    selected_identity UUID;
    expected_verification VARCHAR(32);
BEGIN
    IF NEW.event_type = 'OWNERSHIP_REASSIGNED' THEN
        SELECT * INTO successor FROM progression_subject_identity WHERE id = NEW.identity_id;
        SELECT * INTO predecessor FROM progression_subject_identity WHERE id = NEW.predecessor_identity_id;
        SELECT * INTO approval_row
        FROM progression_subject_reassignment_authorization
        WHERE authorization_id = NEW.reassignment_authorization_id;
        SELECT current_identity_id INTO selected_identity
        FROM progression_subject_current_binding
        WHERE namespace = successor.namespace AND external_id = successor.external_id;

        IF successor.id IS NULL OR predecessor.id IS NULL OR approval_row.authorization_id IS NULL
           OR NEW.aggregate_version IS DISTINCT FROM 0
           OR NEW.previous_identity_class IS DISTINCT FROM 'EXTERNAL'
           OR NEW.new_identity_class IS DISTINCT FROM 'EXTERNAL'
           OR NEW.previous_ownership_status IS DISTINCT FROM 'REVOKED'
           OR NEW.new_ownership_status IS DISTINCT FROM 'DISABLED'
           OR NEW.previous_verification_status IS DISTINCT FROM predecessor.verification_status
           OR NEW.new_verification_status IS DISTINCT FROM successor.verification_status
           OR NEW.previous_target_jogador_id IS DISTINCT FROM predecessor.jogador_id
           OR NEW.new_target_jogador_id IS DISTINCT FROM successor.jogador_id
           OR NEW.previous_target_jogador_id IS NULL
           OR NEW.new_target_jogador_id IS NULL
           OR NEW.previous_target_jogador_id = NEW.new_target_jogador_id
           OR NEW.provenance IS DISTINCT FROM 'LOGOS_OPERATOR_ACTION'
           OR NEW.actor_type IS DISTINCT FROM 'WORKLOAD_OPERATOR'
           OR NEW.actor_id IS NULL OR btrim(NEW.actor_id) = ''
           OR NEW.evidence_type IS DISTINCT FROM 'ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION'
           OR NEW.evidence_reference IS DISTINCT FROM NEW.reassignment_authorization_id::text
           OR NEW.reason IS NULL OR btrim(NEW.reason) = ''
           OR successor.predecessor_identity_id IS DISTINCT FROM predecessor.id
           OR successor.namespace IS DISTINCT FROM predecessor.namespace
           OR successor.external_id IS DISTINCT FROM predecessor.external_id
           OR successor.identity_class IS DISTINCT FROM 'EXTERNAL'
           OR successor.ownership_status IS DISTINCT FROM 'DISABLED'
           OR successor.ownership_version IS DISTINCT FROM 0
           OR successor.jogador_id IS DISTINCT FROM NEW.new_target_jogador_id
           OR predecessor.identity_class IS DISTINCT FROM 'EXTERNAL'
           OR predecessor.ownership_status IS DISTINCT FROM 'REVOKED'
           OR predecessor.ownership_version IS DISTINCT FROM NEW.predecessor_ownership_version
           OR predecessor.jogador_id IS DISTINCT FROM NEW.previous_target_jogador_id
           OR selected_identity IS DISTINCT FROM successor.id
           OR approval_row.reassignment_request_id IS DISTINCT FROM NEW.reassignment_request_id
           OR approval_row.namespace IS DISTINCT FROM successor.namespace
           OR approval_row.external_id IS DISTINCT FROM successor.external_id
           OR approval_row.predecessor_identity_id IS DISTINCT FROM predecessor.id
           OR approval_row.predecessor_ownership_version IS DISTINCT FROM predecessor.ownership_version
           OR approval_row.predecessor_target_jogador_id IS DISTINCT FROM predecessor.jogador_id
           OR approval_row.proposed_successor_target_jogador_id IS DISTINCT FROM successor.jogador_id
           OR approval_row.authorization_id::text IS DISTINCT FROM NEW.evidence_reference
           OR approval_row.reviewer_principal_id IS NOT DISTINCT FROM NEW.actor_id THEN
            RAISE EXCEPTION 'OWNERSHIP_REASSIGNED history must match its successor, predecessor, pointer, and authorization';
        END IF;
        RETURN NULL;
    END IF;

    IF NEW.event_type <> 'OWNERSHIP_TARGET_CORRECTED' THEN
        RETURN NULL;
    END IF;

    SELECT * INTO successor FROM progression_subject_identity WHERE id = NEW.identity_id;
    SELECT * INTO predecessor FROM progression_subject_identity WHERE id = NEW.predecessor_identity_id;
    SELECT current_identity_id INTO selected_identity
    FROM progression_subject_current_binding
    WHERE namespace = successor.namespace AND external_id = successor.external_id;

    expected_verification := CASE predecessor.verification_status
        WHEN 'VERIFIED' THEN 'UNVERIFIED'
        WHEN 'UNVERIFIED' THEN 'UNVERIFIED'
        WHEN 'INVALIDATED' THEN 'INVALIDATED'
        ELSE NULL
    END;

    IF successor.id IS NULL OR predecessor.id IS NULL OR
       NOT EXISTS (
           SELECT 1 FROM progression_subject_target_correction_authorization approval
           WHERE approval.authorization_id = NEW.target_correction_authorization_id
             AND approval.correction_request_id = NEW.correction_request_id
             AND approval.namespace = successor.namespace
             AND approval.external_id = successor.external_id
             AND approval.predecessor_identity_id = predecessor.id
             AND approval.predecessor_ownership_version = predecessor.ownership_version
             AND approval.predecessor_target_jogador_id = predecessor.jogador_id
             AND approval.corrected_target_jogador_id = successor.jogador_id
             AND approval.reviewer_principal_id <> NEW.actor_id
       ) OR predecessor.namespace IS DISTINCT FROM successor.namespace
       OR predecessor.external_id IS DISTINCT FROM successor.external_id
       OR predecessor.predecessor_identity_id IS NOT NULL
       OR predecessor.identity_class IS DISTINCT FROM 'EXTERNAL'
       OR predecessor.ownership_status NOT IN ('ACTIVE', 'DISABLED')
       OR predecessor.ownership_status IS DISTINCT FROM NEW.previous_ownership_status
       OR predecessor.ownership_version IS DISTINCT FROM NEW.predecessor_ownership_version
       OR predecessor.jogador_id IS DISTINCT FROM NEW.previous_target_jogador_id
       OR predecessor.verification_status IS DISTINCT FROM NEW.previous_verification_status
       OR EXISTS (
           SELECT 1 FROM progression_subject_ownership_history transfer
           WHERE transfer.identity_id = predecessor.id
             AND transfer.event_type = 'OWNERSHIP_TRANSFERRED'
       )
       OR NEW.aggregate_version IS DISTINCT FROM 0
       OR NEW.previous_identity_class IS DISTINCT FROM 'EXTERNAL'
       OR NEW.new_identity_class IS DISTINCT FROM 'EXTERNAL'
       OR NEW.previous_ownership_status NOT IN ('ACTIVE', 'DISABLED')
       OR NEW.new_ownership_status IS DISTINCT FROM 'DISABLED'
       OR NEW.previous_verification_status NOT IN ('UNVERIFIED', 'VERIFIED', 'INVALIDATED')
       OR NEW.new_verification_status IS DISTINCT FROM expected_verification
       OR NEW.previous_target_jogador_id IS NULL
       OR NEW.new_target_jogador_id IS NULL
       OR NEW.previous_target_jogador_id = NEW.new_target_jogador_id
       OR successor.predecessor_identity_id IS DISTINCT FROM predecessor.id
       OR successor.namespace IS DISTINCT FROM predecessor.namespace
       OR successor.external_id IS DISTINCT FROM predecessor.external_id
       OR successor.identity_class IS DISTINCT FROM 'EXTERNAL'
       OR successor.ownership_status IS DISTINCT FROM 'DISABLED'
       OR successor.ownership_version IS DISTINCT FROM 0
       OR successor.jogador_id IS DISTINCT FROM NEW.new_target_jogador_id
       OR selected_identity IS DISTINCT FROM successor.id
       OR NEW.provenance IS DISTINCT FROM 'LOGOS_OPERATOR_ACTION'
       OR NEW.actor_type IS DISTINCT FROM 'WORKLOAD_OPERATOR'
       OR NEW.actor_id IS NULL OR btrim(NEW.actor_id) = ''
       OR NEW.evidence_type IS DISTINCT FROM 'ADMINISTRATIVE_TARGET_CORRECTION_AUTHORIZATION'
       OR NEW.evidence_reference IS DISTINCT FROM NEW.target_correction_authorization_id::text
       OR NEW.reason IS NULL OR btrim(NEW.reason) = '' THEN
        RAISE EXCEPTION 'OWNERSHIP_TARGET_CORRECTED history must match its successor, predecessor, pointer, and authorization';
    END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION validate_progression_subject_current_binding_identity()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    selected_identity UUID;
    lineage_cycle BOOLEAN;
    valid_successor BOOLEAN;
    reassignment_classification BOOLEAN;
    correction_classification BOOLEAN;
BEGIN
    SELECT current_identity_id INTO selected_identity
    FROM progression_subject_current_binding
    WHERE namespace = NEW.namespace AND external_id = NEW.external_id;

    IF selected_identity IS DISTINCT FROM NEW.id THEN
        RAISE EXCEPTION 'new subject identity must be selected by its current locator pointer';
    END IF;

    IF NEW.predecessor_identity_id IS NOT NULL THEN
        SELECT EXISTS (
            SELECT 1
            FROM progression_subject_identity predecessor
            JOIN progression_subject_ownership_history event
              ON event.identity_id = NEW.id
             AND event.predecessor_identity_id = predecessor.id
             AND event.event_type = 'OWNERSHIP_REASSIGNED'
             AND event.aggregate_version = 0
            JOIN progression_subject_reassignment_authorization approval
              ON approval.authorization_id = event.reassignment_authorization_id
            WHERE predecessor.id = NEW.predecessor_identity_id
              AND predecessor.namespace = NEW.namespace
              AND predecessor.external_id = NEW.external_id
              AND predecessor.ownership_status = 'REVOKED'
              AND predecessor.ownership_version = event.predecessor_ownership_version
              AND predecessor.jogador_id = event.previous_target_jogador_id
              AND NEW.jogador_id = event.new_target_jogador_id
              AND approval.reassignment_request_id = event.reassignment_request_id
              AND approval.predecessor_identity_id = predecessor.id
              AND approval.predecessor_ownership_version = predecessor.ownership_version
              AND approval.predecessor_target_jogador_id = predecessor.jogador_id
              AND approval.proposed_successor_target_jogador_id = NEW.jogador_id
              AND approval.namespace = NEW.namespace
              AND approval.external_id = NEW.external_id
              AND approval.reviewer_principal_id <> event.actor_id
        ), EXISTS (
            SELECT 1
            FROM progression_subject_identity predecessor
            JOIN progression_subject_ownership_history event
              ON event.identity_id = NEW.id
             AND event.predecessor_identity_id = predecessor.id
             AND event.event_type = 'OWNERSHIP_TARGET_CORRECTED'
             AND event.aggregate_version = 0
            JOIN progression_subject_target_correction_authorization approval
              ON approval.authorization_id = event.target_correction_authorization_id
             AND approval.correction_request_id = event.correction_request_id
            WHERE predecessor.id = NEW.predecessor_identity_id
              AND predecessor.namespace = NEW.namespace
              AND predecessor.external_id = NEW.external_id
              AND predecessor.predecessor_identity_id IS NULL
              AND predecessor.identity_class = 'EXTERNAL'
              AND predecessor.ownership_status IN ('ACTIVE', 'DISABLED')
              AND predecessor.ownership_version = event.predecessor_ownership_version
              AND predecessor.jogador_id = event.previous_target_jogador_id
              AND predecessor.verification_status = event.previous_verification_status
              AND NEW.jogador_id = event.new_target_jogador_id
              AND NEW.identity_class = 'EXTERNAL'
              AND NEW.ownership_status = 'DISABLED'
              AND NEW.ownership_version = 0
              AND approval.namespace = NEW.namespace
              AND approval.external_id = NEW.external_id
              AND approval.predecessor_identity_id = predecessor.id
              AND approval.predecessor_ownership_version = predecessor.ownership_version
              AND approval.predecessor_target_jogador_id = predecessor.jogador_id
              AND approval.corrected_target_jogador_id = NEW.jogador_id
              AND approval.reviewer_principal_id <> event.actor_id
              AND NOT EXISTS (
                  SELECT 1 FROM progression_subject_ownership_history transfer
                  WHERE transfer.identity_id = predecessor.id
                    AND transfer.event_type = 'OWNERSHIP_TRANSFERRED'
              )
        ) INTO reassignment_classification, correction_classification;

        valid_successor := reassignment_classification <> correction_classification;

        IF NOT valid_successor THEN
            RAISE EXCEPTION 'successor identity requires exactly one valid reassignment or target-correction event and authorization';
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

CREATE OR REPLACE FUNCTION validate_progression_subject_current_binding_change()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.current_identity_id IS DISTINCT FROM NEW.current_identity_id
       AND NOT EXISTS (
           SELECT 1
           FROM progression_subject_identity successor
           JOIN progression_subject_ownership_history event
             ON event.identity_id = successor.id
            AND event.predecessor_identity_id = OLD.current_identity_id
           WHERE successor.id = NEW.current_identity_id
             AND successor.predecessor_identity_id = OLD.current_identity_id
             AND successor.namespace = NEW.namespace
             AND successor.external_id = NEW.external_id
             AND (
                 (event.event_type = 'OWNERSHIP_REASSIGNED'
                  AND EXISTS (
                      SELECT 1 FROM progression_subject_reassignment_authorization approval
                      WHERE approval.authorization_id = event.reassignment_authorization_id
                        AND approval.reassignment_request_id = event.reassignment_request_id
                        AND approval.namespace = NEW.namespace
                        AND approval.external_id = NEW.external_id
                  ))
                 OR
                 (event.event_type = 'OWNERSHIP_TARGET_CORRECTED'
                  AND EXISTS (
                      SELECT 1 FROM progression_subject_target_correction_authorization approval
                      WHERE approval.authorization_id = event.target_correction_authorization_id
                        AND approval.correction_request_id = event.correction_request_id
                        AND approval.namespace = NEW.namespace
                        AND approval.external_id = NEW.external_id
                  ))
             )
       ) THEN
        RAISE EXCEPTION 'current binding pointer can move only to its audited successor';
    END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION reject_progression_subject_identity_locator_mutation()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.id IS DISTINCT FROM NEW.id
       OR OLD.namespace IS DISTINCT FROM NEW.namespace
       OR OLD.external_id IS DISTINCT FROM NEW.external_id
       OR OLD.predecessor_identity_id IS DISTINCT FROM NEW.predecessor_identity_id THEN
        RAISE EXCEPTION 'subject identity identity, locator, and lineage are immutable';
    END IF;
    IF EXISTS (
        SELECT 1 FROM progression_subject_identity child
        WHERE child.predecessor_identity_id = OLD.id
    ) AND (OLD.jogador_id IS DISTINCT FROM NEW.jogador_id
        OR OLD.ownership_status IS DISTINCT FROM NEW.ownership_status
        OR OLD.verification_status IS DISTINCT FROM NEW.verification_status
        OR OLD.ownership_version IS DISTINCT FROM NEW.ownership_version) THEN
        RAISE EXCEPTION 'subject identity with a successor preserves its historical ownership facts';
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

CREATE CONSTRAINT TRIGGER trg_progression_subject_target_correction_identity_integrity
    AFTER INSERT ON progression_subject_identity
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION validate_progression_subject_current_binding_identity();

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM progression_subject_current_binding pointer
        LEFT JOIN progression_subject_identity identity
          ON identity.id = pointer.current_identity_id
         AND identity.namespace = pointer.namespace
         AND identity.external_id = pointer.external_id
        WHERE identity.id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM progression_subject_identity identity
        WHERE NOT EXISTS (
            SELECT 1 FROM progression_subject_current_binding pointer
            WHERE pointer.namespace = identity.namespace AND pointer.external_id = identity.external_id
        )
    ) THEN
        RAISE EXCEPTION 'V50 validation failed: current pointer coverage or locator mismatch';
    END IF;
END;
$$;
