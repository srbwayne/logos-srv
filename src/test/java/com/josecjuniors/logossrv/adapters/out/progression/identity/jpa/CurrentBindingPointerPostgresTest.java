package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionSubjectIdentityProvisioningPort;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CurrentBindingPointerPostgresTest {

    private static final String NAMESPACE = "r1-pointer-test";

    @Autowired JdbcTemplate jdbc;
    @Autowired RegistrationUseCase registration;
    @Autowired ProgressionSubjectIdentityProvisioningPort provisioning;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void freshBootstrapUsesV49AndInitialBindingHasPointer() {
        assertThat(jdbc.queryForObject("""
                SELECT version FROM flyway_schema_history
                WHERE success = TRUE AND version IS NOT NULL
                ORDER BY installed_rank DESC LIMIT 1
                """, String.class)).isEqualTo("49");

        var target = newTarget("pointer-first");
        var reference = new ExternalSubjectReference(NAMESPACE, "first-" + UUID.randomUUID());
        provisioning.provision(reference, new SubjectId(target.userId));

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_identity identity
                JOIN progression_subject_current_binding pointer
                  ON pointer.namespace = identity.namespace
                 AND pointer.external_id = identity.external_id
                 AND pointer.current_identity_id = identity.id
                WHERE pointer.namespace = ? AND pointer.external_id = ?
                """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
    }

    @Test
    void controlledSuccessorRowsSupportAtoBtoCWithoutChangingPredecessors() {
        var targetA = newTarget("lineage-a");
        var targetB = newTarget("lineage-b");
        var targetC = newTarget("lineage-c");
        var reference = new ExternalSubjectReference(NAMESPACE, "chain-" + UUID.randomUUID());
        provisioning.provision(reference, new SubjectId(targetA.userId));
        UUID identityA = currentIdentity(reference);
        UUID jogadorA = targetJogador(targetA.userId);

        revoke(identityA, jogadorA, "ACTIVE");
        UUID identityB = createSuccessor(reference, identityA, jogadorA, targetB.jogadorId);
        revoke(identityB, targetB.jogadorId, "DISABLED");
        UUID identityC = createSuccessor(reference, identityB, targetB.jogadorId, targetC.jogadorId);

        assertThat(currentIdentity(reference)).isEqualTo(identityC);
        assertThat(jdbc.queryForList("""
                SELECT id FROM progression_subject_identity
                WHERE namespace = ? AND external_id = ? ORDER BY id
                """, UUID.class, reference.namespace(), reference.externalId()))
                .containsExactlyInAnyOrder(identityA, identityB, identityC);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_identity
                WHERE id = ? AND ownership_status = 'REVOKED' AND jogador_id = ? AND ownership_version = 1
                """, Integer.class, identityA, jogadorA)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_identity
                WHERE id = ? AND ownership_status = 'REVOKED' AND jogador_id = ? AND ownership_version = 1
                """, Integer.class, identityB, targetB.jogadorId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history
                WHERE event_type = 'OWNERSHIP_REASSIGNED' AND identity_id IN (?, ?)
                """, Integer.class, identityB, identityC)).isEqualTo(2);

        assertThatThrownBy(() -> inNewTransaction(() -> jdbc.update("""
                INSERT INTO progression_subject_current_binding (namespace, external_id, current_identity_id)
                VALUES ('wrong-locator', ?, ?)
                """, "mismatch-" + UUID.randomUUID(), identityB)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lineageRejectsSelfLinksAndBranches() {
        var targetA = newTarget("lineage-constraints");
        var reference = new ExternalSubjectReference(NAMESPACE, "branch-" + UUID.randomUUID());
        provisioning.provision(reference, new SubjectId(targetA.userId));
        UUID predecessor = currentIdentity(reference);
        UUID jogadorId = targetJogador(targetA.userId);

        UUID selfIdentity = UUID.randomUUID();
        assertThatThrownBy(() -> inNewTransaction(() -> jdbc.update(identityInsertSql(),
                selfIdentity, reference.namespace(), reference.externalId(), jogadorId,
                "EXTERNAL", "ACTIVE", "UNVERIFIED", 0L, selfIdentity)))
                .isInstanceOf(DataIntegrityViolationException.class);

        UUID firstBranch = UUID.randomUUID();
        UUID secondBranch = UUID.randomUUID();
        assertThatThrownBy(() -> inNewTransaction(() -> {
            jdbc.update(identityInsertSql(), firstBranch, reference.namespace(), reference.externalId(), jogadorId,
                    "EXTERNAL", "DISABLED", "UNVERIFIED", 0L, predecessor);
            jdbc.update(identityInsertSql(), secondBranch, reference.namespace(), reference.externalId(), jogadorId,
                    "EXTERNAL", "DISABLED", "UNVERIFIED", 0L, predecessor);
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pointerRejectsDanglingTargetsDuplicateCurrentIdentityAndDeletion() {
        var target = newTarget("pointer-constraints");
        var reference = new ExternalSubjectReference(NAMESPACE, "pointer-" + UUID.randomUUID());
        provisioning.provision(reference, new SubjectId(target.userId));
        UUID identityId = currentIdentity(reference);

        assertThatThrownBy(() -> inNewTransaction(() -> {
            jdbc.execute("SET CONSTRAINTS fk_progression_subject_current_binding_identity_locator IMMEDIATE");
            jdbc.update("""
                    INSERT INTO progression_subject_current_binding (namespace, external_id, current_identity_id)
                    VALUES ('dangling', ?, ?)
                    """, UUID.randomUUID().toString(), UUID.randomUUID());
        })).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> inNewTransaction(() -> jdbc.update("""
                INSERT INTO progression_subject_current_binding (namespace, external_id, current_identity_id)
                VALUES ('duplicate-current', ?, ?)
                """, UUID.randomUUID().toString(), identityId)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> inNewTransaction(() -> jdbc.update("""
                DELETE FROM progression_subject_current_binding
                WHERE namespace = ? AND external_id = ?
                """, reference.namespace(), reference.externalId())))
                .isInstanceOf(org.springframework.jdbc.UncategorizedSQLException.class)
                .hasMessageContaining("current subject binding pointers cannot be deleted");
    }

    @Test
    void predecessorMustUseSameLocator() {
        var target = newTarget("lineage-locator");
        var reference = new ExternalSubjectReference(NAMESPACE, "locator-source-" + UUID.randomUUID());
        provisioning.provision(reference, new SubjectId(target.userId));
        UUID predecessor = currentIdentity(reference);

        assertThatThrownBy(() -> inNewTransaction(() -> {
            jdbc.execute("SET CONSTRAINTS fk_progression_subject_identity_predecessor_locator IMMEDIATE");
            jdbc.update(identityInsertSql(), UUID.randomUUID(), reference.namespace(),
                    "different-locator-" + UUID.randomUUID(), targetJogador(target.userId),
                    "EXTERNAL", "DISABLED", "UNVERIFIED", 0L, predecessor);
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reassignmentHistoryRejectsNullPreviousIdentityClass() {
        assertReassignmentEventRejected(null, "REVOKED", "UNVERIFIED",
                "ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION");
    }

    @Test
    void reassignmentHistoryRejectsNullPreviousOwnershipStatus() {
        assertReassignmentEventRejected("EXTERNAL", null, "UNVERIFIED",
                "ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION");
    }

    @Test
    void reassignmentHistoryRejectsNullPreviousVerificationStatus() {
        assertReassignmentEventRejected("EXTERNAL", "REVOKED", null,
                "ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION");
    }

    @Test
    void reassignmentHistoryRejectsNullEvidenceType() {
        assertReassignmentEventRejected("EXTERNAL", "REVOKED", "UNVERIFIED", null);
    }

    @Test
    void reassignmentHistoryRejectsWrongNonNullSnapshot() {
        assertReassignmentEventRejected("EXTERNAL", "ACTIVE", "UNVERIFIED",
                "ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION");
    }

    private void assertReassignmentEventRejected(String previousIdentityClass,
                                                 String previousOwnershipStatus,
                                                 String previousVerificationStatus,
                                                 String evidenceType) {
        var source = newTarget("reassignment-null-source-" + UUID.randomUUID());
        var destination = newTarget("reassignment-null-destination-" + UUID.randomUUID());
        var reference = new ExternalSubjectReference(NAMESPACE, "invalid-event-" + UUID.randomUUID());
        provisioning.provision(reference, new SubjectId(source.userId));
        UUID identityId = currentIdentity(reference);
        UUID oldTarget = targetJogador(source.userId);
        UUID newTarget = destination.jogadorId;
        UUID authorizationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        assertThatThrownBy(() -> inNewTransaction(() -> {
            jdbc.update("""
                    INSERT INTO progression_subject_reassignment_authorization
                        (authorization_id, reassignment_request_id, namespace, external_id,
                         predecessor_identity_id, predecessor_ownership_version,
                         predecessor_target_jogador_id, proposed_successor_target_jogador_id,
                         recovery_basis, reviewed_case_reference, reviewer_principal_id)
                    VALUES (?, ?, ?, ?, ?, 0, ?, ?, 'reviewed recovery', 'case-reference', 'reviewer-principal')
                    """, authorizationId, requestId, reference.namespace(), reference.externalId(),
                    identityId, oldTarget, newTarget);
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
                    VALUES (?, ?, 100, 'OWNERSHIP_REASSIGNED', ?, 'EXTERNAL', ?, ?, ?, 'DISABLED', ?, 'UNVERIFIED',
                            'LOGOS_OPERATOR_ACTION', 'WORKLOAD_OPERATOR', 'executor-principal', ?, ?,
                            'structural rejection test', clock_timestamp(), ?, 0, ?, ?)
                    """, UUID.randomUUID(), identityId, previousIdentityClass, oldTarget, newTarget,
                    previousOwnershipStatus, previousVerificationStatus, evidenceType, authorizationId.toString(),
                    identityId, requestId, authorizationId);
        })).isInstanceOf(DataAccessException.class);
    }

    private UUID createSuccessor(ExternalSubjectReference reference, UUID predecessorId,
                                  UUID oldTarget, UUID newTarget) {
        UUID successorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        inNewTransaction(() -> {
            jdbc.update("""
                    INSERT INTO progression_subject_reassignment_authorization
                        (authorization_id, reassignment_request_id, namespace, external_id,
                         predecessor_identity_id, predecessor_ownership_version,
                         predecessor_target_jogador_id, proposed_successor_target_jogador_id,
                         recovery_basis, reviewed_case_reference, reviewer_principal_id)
                    VALUES (?, ?, ?, ?, ?, 1, ?, ?, 'reviewed recovery', 'case-reference', 'reviewer-principal')
                    """, authorizationId, requestId, reference.namespace(), reference.externalId(),
                    predecessorId, oldTarget, newTarget);
            jdbc.update(identityInsertSql(), successorId, reference.namespace(), reference.externalId(), newTarget,
                    "EXTERNAL", "DISABLED", "UNVERIFIED", 0L, predecessorId);
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
                            'REVOKED', 'DISABLED', 'UNVERIFIED', 'UNVERIFIED',
                            'LOGOS_OPERATOR_ACTION', 'WORKLOAD_OPERATOR', 'executor-principal',
                            'ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION', ?, 'controlled fixture',
                            clock_timestamp(), ?, 1, ?, ?)
                    """, UUID.randomUUID(), successorId, oldTarget, newTarget, authorizationId.toString(),
                    predecessorId, requestId, authorizationId);
            assertThat(jdbc.update("""
                    UPDATE progression_subject_current_binding
                    SET current_identity_id = ?
                    WHERE namespace = ? AND external_id = ? AND current_identity_id = ?
                    """, successorId, reference.namespace(), reference.externalId(), predecessorId)).isEqualTo(1);
        });
        return successorId;
    }

    private void revoke(UUID identityId, UUID target, String previousStatus) {
        inNewTransaction(() -> {
            assertThat(jdbc.update("""
                    UPDATE progression_subject_identity
                    SET ownership_status = 'REVOKED', ownership_version = 1
                    WHERE id = ? AND ownership_status = ?
                    """, identityId, previousStatus)).isEqualTo(1);
            jdbc.update("""
                    INSERT INTO progression_subject_ownership_history
                        (id, identity_id, aggregate_version, event_type,
                         previous_identity_class, new_identity_class,
                         previous_target_jogador_id, new_target_jogador_id,
                         previous_ownership_status, new_ownership_status,
                         previous_verification_status, new_verification_status,
                         provenance, actor_type, actor_id, reason, effective_at)
                    VALUES (?, ?, 1, 'OWNERSHIP_REVOKED', 'EXTERNAL', 'EXTERNAL', ?, ?,
                            ?, 'REVOKED', 'UNVERIFIED', 'UNVERIFIED',
                            'LOGOS_OPERATOR_ACTION', 'WORKLOAD_OPERATOR', 'fixture-operator',
                            'controlled test revocation', clock_timestamp())
                    """, UUID.randomUUID(), identityId, target, target, previousStatus);
        });
    }

    private UUID currentIdentity(ExternalSubjectReference reference) {
        return jdbc.queryForObject("""
                SELECT current_identity_id FROM progression_subject_current_binding
                WHERE namespace = ? AND external_id = ?
                """, UUID.class, reference.namespace(), reference.externalId());
    }

    private UUID targetJogador(UUID userId) {
        return jdbc.queryForObject("SELECT id FROM jogador WHERE user_id = ?", UUID.class, userId);
    }

    private Target newTarget(String prefix) {
        var registered = registration.register(new RegistrationCommand(
                prefix + "-" + UUID.randomUUID() + "@example.test", "password", prefix));
        UUID userId = registered.user().getId().getValue();
        return new Target(userId, targetJogador(userId));
    }

    private String identityInsertSql() {
        return """
                INSERT INTO progression_subject_identity
                    (id, namespace, external_id, jogador_id, identity_class, ownership_status,
                     verification_status, ownership_version, predecessor_identity_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
    }

    private void inNewTransaction(Runnable work) {
        var definition = new TransactionTemplate(transactionManager);
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        definition.executeWithoutResult(status -> work.run());
    }

    private record Target(UUID userId, UUID jogadorId) { }
}
