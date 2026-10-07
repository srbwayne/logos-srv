package com.josecjuniors.logossrv.adapters.out.subjectownership.jpa;

import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentity;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentityJpaRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentAuthorization;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.IdentityClass;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipProvenance;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipStatus;
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
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaSubjectOwnershipReassignmentStore implements SubjectOwnershipReassignmentStore {
    private static final String AUTH_SELECT = """
            SELECT authorization_id, reassignment_request_id, namespace, external_id,
                   predecessor_identity_id, predecessor_ownership_version,
                   predecessor_target_jogador_id, proposed_successor_target_jogador_id,
                   recovery_basis, reviewed_case_reference, reviewer_principal_id, reviewed_at
            FROM progression_subject_reassignment_authorization
            WHERE %s = ?
            """;
    private static final RowMapper<SubjectOwnershipReassignmentAuthorization> AUTH_MAPPER = (rs, row) ->
            new SubjectOwnershipReassignmentAuthorization(rs.getObject("authorization_id", UUID.class),
                    rs.getObject("reassignment_request_id", UUID.class),
                    new ExternalSubjectReference(rs.getString("namespace"), rs.getString("external_id")),
                    rs.getObject("predecessor_identity_id", UUID.class), rs.getLong("predecessor_ownership_version"),
                    rs.getObject("predecessor_target_jogador_id", UUID.class),
                    rs.getObject("proposed_successor_target_jogador_id", UUID.class), rs.getString("recovery_basis"),
                    rs.getString("reviewed_case_reference"), rs.getString("reviewer_principal_id"),
                    rs.getTimestamp("reviewed_at").toInstant());

    private final JdbcTemplate jdbc;
    private final ProgressionSubjectIdentityJpaRepository identities;

    public JpaSubjectOwnershipReassignmentStore(JdbcTemplate jdbc,
            ProgressionSubjectIdentityJpaRepository identities) {
        this.jdbc = jdbc;
        this.identities = identities;
    }

    @Override public Optional<SubjectOwnershipReassignmentAuthorization> findAuthorizationById(UUID id) {
        return queryOne(AUTH_SELECT.formatted("authorization_id"), id, AUTH_MAPPER);
    }

    @Override public Optional<SubjectOwnershipReassignmentAuthorization> findAuthorizationByRequestId(UUID id) {
        return queryOne(AUTH_SELECT.formatted("reassignment_request_id"), id, AUTH_MAPPER);
    }

    @Override
    @Transactional
    public SubjectOwnershipReassignmentAuthorization insertAuthorizationIfAbsent(
            SubjectOwnershipReassignmentAuthorization proposed) {
        jdbc.update("""
                INSERT INTO progression_subject_reassignment_authorization
                    (authorization_id, reassignment_request_id, namespace, external_id,
                     predecessor_identity_id, predecessor_ownership_version,
                     predecessor_target_jogador_id, proposed_successor_target_jogador_id,
                     recovery_basis, reviewed_case_reference, reviewer_principal_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """, proposed.authorizationId(), proposed.reassignmentRequestId(), proposed.reference().namespace(),
                proposed.reference().externalId(), proposed.predecessorIdentityId(),
                proposed.predecessorOwnershipVersion(), proposed.predecessorTargetJogadorId(),
                proposed.proposedSuccessorTargetJogadorId(), proposed.recoveryBasis(),
                proposed.reviewedCaseReference(), proposed.reviewerPrincipalId());
        return findAuthorizationByRequestId(proposed.reassignmentRequestId()).orElseThrow(
                () -> new IllegalStateException("Authorization insert did not produce a request record"));
    }

    @Override
    @Transactional
    public Optional<ReassignmentPredecessor> lockCurrentPredecessor(ExternalSubjectReference reference) {
        var id = identities.lockCurrentIdentityId(reference.namespace(), reference.externalId());
        if (id.isEmpty()) {
            if (identities.existsByNamespaceAndExternalId(reference.namespace(), reference.externalId()))
                throw new DataIntegrityViolationException("Subject identity exists without a current-binding pointer");
            return Optional.empty();
        }
        ProgressionSubjectIdentity row = identities.findCurrentIdentityByIdForUpdate(id.get())
                .orElseThrow(() -> new IllegalStateException("Current pointer references a missing identity"));
        if (!reference.namespace().equals(row.getNamespace()) || !reference.externalId().equals(row.getExternalId()))
            throw new IllegalStateException("Current pointer locator does not match identity");
        return Optional.of(new ReassignmentPredecessor(row.getId(), reference, row.getJogador().getId().getValue(),
                row.getIdentityClass().name(), row.getOwnershipStatus().name(), row.getVerificationStatus(),
                row.getOwnershipVersion()));
    }

    @Override
    public Optional<ReassignmentEvent> findCompletedReassignmentByRequestId(UUID requestId) {
        return queryOne("""
                SELECT h.identity_id, h.aggregate_version, i.namespace, i.external_id,
                       h.previous_identity_class, h.new_identity_class, h.new_target_jogador_id,
                       h.new_ownership_status, h.new_verification_status,
                       h.previous_target_jogador_id, h.previous_ownership_status,
                       h.previous_verification_status, h.predecessor_ownership_version,
                       h.reason, h.evidence_type, h.evidence_reference, h.provenance,
                       h.actor_type, h.actor_id, h.reassignment_request_id,
                       h.reassignment_authorization_id, h.predecessor_identity_id
                FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id = h.identity_id
                WHERE h.event_type = 'OWNERSHIP_REASSIGNED' AND h.reassignment_request_id = ?
                """, requestId, (rs, row) -> event(rs));
    }

    @Override public boolean targetExists(UUID targetId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM jogador WHERE id = ?)",
                Boolean.class, targetId));
    }

    @Override
    public void createSuccessorAndHistory(ReassignmentPredecessor predecessor, UUID successorId, UUID targetId,
            VerificationStatus verification, UUID requestId, UUID authorizationId, String reason,
            AuthorizedSubjectOwnershipOperator executor, Instant effectiveAt) {
        jdbc.update("""
                INSERT INTO progression_subject_identity
                    (id, namespace, external_id, jogador_id, identity_class, ownership_status,
                     verification_status, ownership_version, predecessor_identity_id)
                VALUES (?, ?, ?, ?, 'EXTERNAL', 'DISABLED', ?, 0, ?)
                """, successorId, predecessor.reference().namespace(), predecessor.reference().externalId(),
                targetId, verification.name(), predecessor.id());
        jdbc.update("""
                INSERT INTO progression_subject_ownership_history
                    (id, identity_id, aggregate_version, event_type,
                     previous_identity_class, new_identity_class,
                     previous_target_jogador_id, new_target_jogador_id,
                     previous_ownership_status, new_ownership_status,
                     previous_verification_status, new_verification_status,
                     provenance, actor_type, actor_id, evidence_type, evidence_reference,
                     reason, effective_at, predecessor_identity_id, predecessor_ownership_version,
                     reassignment_request_id, reassignment_authorization_id)
                VALUES (?, ?, 0, 'OWNERSHIP_REASSIGNED', 'EXTERNAL', 'EXTERNAL', ?, ?,
                        'REVOKED', 'DISABLED', ?, ?, 'LOGOS_OPERATOR_ACTION', 'WORKLOAD_OPERATOR', ?,
                        'ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION', ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), successorId, predecessor.targetJogadorId(), targetId,
                predecessor.verificationStatus().name(), verification.name(), executor.principalId(),
                authorizationId.toString(), reason, Timestamp.from(effectiveAt), predecessor.id(), predecessor.ownershipVersion(),
                requestId, authorizationId);
    }

    @Override
    public int switchCurrentPointer(ExternalSubjectReference reference, UUID predecessorId, UUID successorId) {
        return jdbc.update("""
                UPDATE progression_subject_current_binding SET current_identity_id = ?
                WHERE namespace = ? AND external_id = ? AND current_identity_id = ?
                """, successorId, reference.namespace(), reference.externalId(), predecessorId);
    }

    private <T> Optional<T> queryOne(String sql, UUID key, RowMapper<T> mapper) {
        List<T> rows = jdbc.query(sql, mapper, key);
        return rows.stream().findFirst();
    }

    private static ReassignmentEvent event(ResultSet rs) throws SQLException {
        long predecessorVersion = rs.getLong("predecessor_ownership_version");
        return new ReassignmentEvent(rs.getObject("identity_id", UUID.class), rs.getLong("aggregate_version"),
                new ExternalSubjectReference(rs.getString("namespace"), rs.getString("external_id")),
                rs.getString("previous_identity_class"), rs.getString("new_identity_class"),
                rs.getObject("new_target_jogador_id", UUID.class), rs.getString("new_ownership_status"),
                VerificationStatus.valueOf(rs.getString("new_verification_status")),
                rs.getObject("previous_target_jogador_id", UUID.class), rs.getString("previous_ownership_status"),
                VerificationStatus.valueOf(rs.getString("previous_verification_status")), predecessorVersion,
                rs.getString("reason"), rs.getString("evidence_type"), rs.getString("evidence_reference"),
                rs.getString("provenance"), rs.getString("actor_type"), rs.getString("actor_id"),
                rs.getObject("reassignment_request_id", UUID.class),
                rs.getObject("reassignment_authorization_id", UUID.class),
                rs.getObject("predecessor_identity_id", UUID.class));
    }
}
