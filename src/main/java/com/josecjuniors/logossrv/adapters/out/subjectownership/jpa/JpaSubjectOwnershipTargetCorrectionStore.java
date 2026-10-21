package com.josecjuniors.logossrv.adapters.out.subjectownership.jpa;

import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentity;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentityJpaRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionAuthorization;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.TargetCorrectionAuthorizationStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubjectOwnershipTargetCorrectionStore
        implements TargetCorrectionAuthorizationStore, SubjectOwnershipTargetCorrectionStore {
    private static final String AUTH_SELECT = """
            SELECT authorization_id, correction_request_id, namespace, external_id,
                   predecessor_identity_id, predecessor_ownership_version,
                   predecessor_target_jogador_id, corrected_target_jogador_id,
                   correction_basis, source_assertion_reference, authoritative_fact_reference,
                   reviewed_case_reference, reviewer_principal_id, reviewed_at
            FROM progression_subject_target_correction_authorization WHERE %s = ?
            """;
    private static final RowMapper<SubjectOwnershipTargetCorrectionAuthorization> AUTH_MAPPER = (rs, row) ->
            new SubjectOwnershipTargetCorrectionAuthorization(rs.getObject("authorization_id", UUID.class),
                    rs.getObject("correction_request_id", UUID.class),
                    new ExternalSubjectReference(rs.getString("namespace"), rs.getString("external_id")),
                    rs.getObject("predecessor_identity_id", UUID.class),
                    rs.getLong("predecessor_ownership_version"),
                    rs.getObject("predecessor_target_jogador_id", UUID.class),
                    rs.getObject("corrected_target_jogador_id", UUID.class), rs.getString("correction_basis"),
                    rs.getString("source_assertion_reference"), rs.getString("authoritative_fact_reference"),
                    rs.getString("reviewed_case_reference"), rs.getString("reviewer_principal_id"),
                    rs.getTimestamp("reviewed_at").toInstant());

    private final JdbcTemplate jdbc;
    private final ProgressionSubjectIdentityJpaRepository identities;

    public JpaSubjectOwnershipTargetCorrectionStore(JdbcTemplate jdbc,
            ProgressionSubjectIdentityJpaRepository identities) {
        this.jdbc = jdbc;
        this.identities = identities;
    }

    @Override public Optional<SubjectOwnershipTargetCorrectionAuthorization> findAuthorizationById(UUID id) {
        return queryOne(AUTH_SELECT.formatted("authorization_id"), id, AUTH_MAPPER);
    }

    @Override public Optional<SubjectOwnershipTargetCorrectionAuthorization> findAuthorizationByRequestId(UUID id) {
        return queryOne(AUTH_SELECT.formatted("correction_request_id"), id, AUTH_MAPPER);
    }

    @Override public SubjectOwnershipTargetCorrectionAuthorization insertAuthorizationIfAbsent(
            SubjectOwnershipTargetCorrectionAuthorization proposed) {
        jdbc.update("""
                INSERT INTO progression_subject_target_correction_authorization
                    (authorization_id, correction_request_id, namespace, external_id,
                     predecessor_identity_id, predecessor_ownership_version,
                     predecessor_target_jogador_id, corrected_target_jogador_id,
                     correction_basis, source_assertion_reference, authoritative_fact_reference,
                     reviewed_case_reference, reviewer_principal_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """, proposed.authorizationId(), proposed.correctionRequestId(),
                proposed.reference().namespace(), proposed.reference().externalId(),
                proposed.predecessorIdentityId(), proposed.predecessorOwnershipVersion(),
                proposed.predecessorTargetJogadorId(), proposed.correctedTargetJogadorId(),
                proposed.correctionBasis(), proposed.sourceAssertionReference(),
                proposed.authoritativeFactReference(), proposed.reviewedCaseReference(),
                proposed.reviewerPrincipalId());
        return findAuthorizationByRequestId(proposed.correctionRequestId()).orElseThrow(
                () -> new IllegalStateException("Authorization insert did not produce a request record"));
    }

    @Override public Optional<UUID> lockCurrentPointer(ExternalSubjectReference reference) {
        var id = identities.lockCurrentIdentityId(reference.namespace(), reference.externalId());
        if (id.isEmpty() && identities.existsByNamespaceAndExternalId(reference.namespace(), reference.externalId()))
            throw new DataIntegrityViolationException("Subject identity exists without a current-binding pointer");
        return id;
    }

    @Override public Optional<CorrectionPredecessor> lockSelectedIdentity(
            ExternalSubjectReference reference, UUID identityId) {
        ProgressionSubjectIdentity row = identities.findCurrentIdentityByIdForUpdate(identityId)
                .orElseThrow(() -> new DataIntegrityViolationException("Current pointer references a missing identity"));
        if (!reference.namespace().equals(row.getNamespace()) || !reference.externalId().equals(row.getExternalId()))
            throw new DataIntegrityViolationException("Current pointer locator does not match identity");
        return Optional.of(new CorrectionPredecessor(row.getId(), reference, row.getJogador().getId().getValue(),
                row.getIdentityClass().name(), row.getOwnershipStatus().name(), row.getVerificationStatus(),
                row.getOwnershipVersion(), row.getPredecessorIdentityId()));
    }

    @Override public Optional<CorrectionEvent> findCompletedCorrectionByRequestId(UUID requestId) {
        return queryOne("""
                SELECT h.identity_id, h.aggregate_version, i.namespace, i.external_id,
                       i.predecessor_identity_id, h.predecessor_ownership_version,
                       h.previous_target_jogador_id, h.new_target_jogador_id,
                       h.previous_identity_class, h.new_identity_class,
                       h.previous_ownership_status, h.new_ownership_status,
                       h.previous_verification_status, h.new_verification_status,
                       h.provenance, h.actor_type, h.actor_id, h.evidence_type, h.evidence_reference,
                       h.reason, h.correction_request_id, h.target_correction_authorization_id
                FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id = h.identity_id
                WHERE h.event_type = 'OWNERSHIP_TARGET_CORRECTED' AND h.correction_request_id = ?
                """, requestId, (rs, row) -> event(rs));
    }

    @Override public boolean hasTransferEvent(UUID identityId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM progression_subject_ownership_history
                               WHERE identity_id = ? AND event_type = 'OWNERSHIP_TRANSFERRED')
                """, Boolean.class, identityId));
    }

    @Override public boolean hasSuccessor(UUID identityId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM progression_subject_identity WHERE predecessor_identity_id = ?)
                """, Boolean.class, identityId));
    }

    @Override public boolean targetExists(UUID targetId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM jogador WHERE id = ?)",
                Boolean.class, targetId));
    }

    @Override public void insertSuccessor(CorrectionPredecessor predecessor, UUID successorId, UUID targetId,
            VerificationStatus verification) {
        jdbc.update("""
                INSERT INTO progression_subject_identity
                    (id, namespace, external_id, jogador_id, identity_class, ownership_status,
                     verification_status, ownership_version, predecessor_identity_id)
                VALUES (?, ?, ?, ?, 'EXTERNAL', 'DISABLED', ?, 0, ?)
                """, successorId, predecessor.reference().namespace(), predecessor.reference().externalId(),
                targetId, verification.name(), predecessor.id());
    }

    @Override public void insertCorrectionHistory(CorrectionPredecessor predecessor, UUID successorId, UUID targetId,
            VerificationStatus nextVerification, UUID requestId, UUID authorizationId, String reason,
            AuthorizedSubjectOwnershipOperator executor, Instant effectiveAt) {
        jdbc.update("""
                INSERT INTO progression_subject_ownership_history
                    (id, identity_id, aggregate_version, event_type,
                     previous_identity_class, new_identity_class,
                     previous_target_jogador_id, new_target_jogador_id,
                     previous_ownership_status, new_ownership_status,
                     previous_verification_status, new_verification_status,
                     provenance, actor_type, actor_id, evidence_type, evidence_reference,
                     reason, effective_at, predecessor_identity_id, predecessor_ownership_version,
                     correction_request_id, target_correction_authorization_id)
                VALUES (?, ?, 0, 'OWNERSHIP_TARGET_CORRECTED', 'EXTERNAL', 'EXTERNAL', ?, ?,
                        ?, 'DISABLED', ?, ?, 'LOGOS_OPERATOR_ACTION', 'WORKLOAD_OPERATOR', ?,
                        'ADMINISTRATIVE_TARGET_CORRECTION_AUTHORIZATION', ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), successorId, predecessor.targetJogadorId(), targetId,
                predecessor.ownershipStatus(), predecessor.verificationStatus().name(), nextVerification.name(),
                executor.principalId(), authorizationId.toString(), reason, Timestamp.from(effectiveAt),
                predecessor.id(), predecessor.ownershipVersion(), requestId, authorizationId);
    }

    @Override public int compareAndSetCurrentPointerForTargetCorrection(ExternalSubjectReference reference,
            UUID predecessorId, UUID successorId) {
        return jdbc.update("""
                UPDATE progression_subject_current_binding SET current_identity_id = ?
                WHERE namespace = ? AND external_id = ? AND current_identity_id = ?
                """, successorId, reference.namespace(), reference.externalId(), predecessorId);
    }

    private <T> Optional<T> queryOne(String sql, UUID key, RowMapper<T> mapper) {
        List<T> rows = jdbc.query(sql, mapper, key);
        return rows.stream().findFirst();
    }

    private static CorrectionEvent event(ResultSet rs) throws SQLException {
        return new CorrectionEvent(rs.getObject("identity_id", UUID.class), rs.getLong("aggregate_version"),
                new ExternalSubjectReference(rs.getString("namespace"), rs.getString("external_id")),
                rs.getObject("predecessor_identity_id", UUID.class),
                rs.getLong("predecessor_ownership_version"),
                rs.getObject("previous_target_jogador_id", UUID.class),
                rs.getObject("new_target_jogador_id", UUID.class), rs.getString("previous_identity_class"),
                rs.getString("new_identity_class"), rs.getString("previous_ownership_status"),
                rs.getString("new_ownership_status"),
                VerificationStatus.valueOf(rs.getString("previous_verification_status")),
                VerificationStatus.valueOf(rs.getString("new_verification_status")), rs.getString("provenance"),
                rs.getString("actor_type"), rs.getString("actor_id"), rs.getString("evidence_type"),
                rs.getString("evidence_reference"), rs.getString("reason"),
                rs.getObject("correction_request_id", UUID.class),
                rs.getObject("target_correction_authorization_id", UUID.class));
    }
}
