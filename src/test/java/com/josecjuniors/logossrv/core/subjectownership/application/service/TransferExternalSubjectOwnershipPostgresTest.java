package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.progression.application.service.ProvisionCurrentExternalSubjectIdentityService;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.DisableExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.DisableExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.TransferExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.TransferExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@FreshPostgresIntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=subject-ownership-transfer-c1b")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferExternalSubjectOwnershipPostgresTest {
    @Autowired TransferExternalSubjectOwnershipUseCase transfer;
    @Autowired RevokeExternalSubjectOwnershipUseCase revoke;
    @Autowired DisableExternalSubjectOwnershipUseCase disable;
    @Autowired InvalidateExternalSubjectOwnershipUseCase invalidate;
    @Autowired VerifyExternalSubjectOwnershipUseCase verify;
    @Autowired RegistrationUseCase registration;
    @Autowired ProvisionCurrentExternalSubjectIdentityService provisioning;
    @Autowired JdbcTemplate jdbc;
    @MockBean SubjectOwnershipOperatorContext operatorContext;

    @BeforeEach void authorize() {
        when(operatorContext.authorizeForNamespace("lifeos"))
                .thenReturn(new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD,
                        "verified-workload-transfer", "lifeos"));
    }

    private ExternalSubjectReference createIdentity(String verification, boolean disabled) {
        String email = "transfer-source-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "Transfer " + UUID.randomUUID()));
        var reference = new ExternalSubjectReference("lifeos", "external-" + UUID.randomUUID());
        provisioning.provision(email, reference);
        long version = 0;
        if (!verification.equals("UNVERIFIED")) {
            verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, version,
                    "initial-review", "verify-case", "initial verification"));
            version++;
        }
        if (verification.equals("INVALIDATED")) {
            invalidate.invalidate(new InvalidateExternalSubjectOwnershipCommand(reference, version,
                    "invalidate-review", "invalidate-case", "verification withdrawn"));
            version++;
        }
        if (disabled) {
            disable.disable(new DisableExternalSubjectOwnershipCommand(reference, version, "administrative suspension"));
        }
        return reference;
    }

    private UUID createTarget() {
        String email = "transfer-target-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "Transfer target " + UUID.randomUUID()));
        return jdbc.queryForObject("SELECT id FROM jogador WHERE user_id=(SELECT id FROM app_user WHERE email=?)",
                UUID.class, email);
    }

    private TransferExternalSubjectOwnershipCommand command(ExternalSubjectReference reference, long version,
            UUID target, String evidence, String reason) {
        return new TransferExternalSubjectOwnershipCommand(reference, version, target, evidence, reason);
    }

    @Test void transfersAllSixEligibleShapesWithFullHistoryAndFixedEvidence() {
        for (String ownership : List.of("ACTIVE", "DISABLED")) {
            for (String verification : List.of("UNVERIFIED", "VERIFIED", "INVALIDATED")) {
                var reference = createIdentity(verification, ownership.equals("DISABLED"));
                UUID targetB = createTarget();
                var before = identity(reference);
                var oldHistory = history(reference);
                long version = ((Number) before.get("ownership_version")).longValue();
                String expectedVerification = verification.equals("VERIFIED") ? "UNVERIFIED" : verification;

                transfer.transfer(command(reference, version, targetB, "  consent-" + verification + "  ",
                        "  accepted handoff " + verification + "  "));

                var after = identity(reference);
                assertThat(after).containsEntry("id", before.get("id"))
                        .containsEntry("namespace", before.get("namespace"))
                        .containsEntry("external_id", before.get("external_id"))
                        .containsEntry("identity_class", "EXTERNAL")
                        .containsEntry("ownership_status", ownership)
                        .containsEntry("verification_status", expectedVerification)
                        .containsEntry("jogador_id", targetB)
                        .containsEntry("ownership_version", version + 1);

                var event = transferEvent(reference, version + 1);
                assertThat(event).containsEntry("aggregate_version", version + 1)
                        .containsEntry("event_type", "OWNERSHIP_TRANSFERRED")
                        .containsEntry("previous_identity_class", "EXTERNAL")
                        .containsEntry("new_identity_class", "EXTERNAL")
                        .containsEntry("previous_target_jogador_id", before.get("jogador_id"))
                        .containsEntry("new_target_jogador_id", targetB)
                        .containsEntry("previous_ownership_status", ownership)
                        .containsEntry("new_ownership_status", ownership)
                        .containsEntry("previous_verification_status", verification)
                        .containsEntry("new_verification_status", expectedVerification)
                        .containsEntry("provenance", "LOGOS_OPERATOR_ACTION")
                        .containsEntry("actor_type", "WORKLOAD_OPERATOR")
                        .containsEntry("actor_id", "verified-workload-transfer")
                        .containsEntry("evidence_type", "BILATERAL_TRANSFER_CONSENT")
                        .containsEntry("evidence_reference", "consent-" + verification)
                        .containsEntry("reason", "accepted handoff " + verification);
                assertThat(event.get("effective_at")).isNotNull();
                assertThat(event.get("recorded_at")).isNotNull();
                var newHistory = history(reference);
                assertThat(newHistory).hasSize(oldHistory.size() + 1);
                assertThat(newHistory.subList(0, oldHistory.size())).isEqualTo(oldHistory);
            }
        }
    }

    @Test void exactEventBackedReplayIsAZeroMutationAndRejectsMismatchedPayloads() {
        var reference = createIdentity("VERIFIED", false);
        UUID targetB = createTarget();
        UUID targetC = createTarget();
        var first = command(reference, 1, targetB, "consent-ref", "approved transfer");
        transfer.transfer(first);
        var identityBeforeReplay = identity(reference);
        var historyBeforeReplay = history(reference);

        transfer.transfer(first);
        assertThat(identity(reference)).isEqualTo(identityBeforeReplay);
        assertThat(history(reference)).isEqualTo(historyBeforeReplay);

        assertThatThrownBy(() -> transfer.transfer(command(reference, 1, targetB, "other-evidence", "approved transfer")))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        assertThatThrownBy(() -> transfer.transfer(command(reference, 1, targetB, "consent-ref", "other reason")))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        assertThatThrownBy(() -> transfer.transfer(command(reference, 1, targetC, "consent-ref", "approved transfer")))
                .isInstanceOf(SubjectOwnershipVersionConflictException.class);
        assertThat(identity(reference)).isEqualTo(identityBeforeReplay);
        assertThat(history(reference)).isEqualTo(historyBeforeReplay);
    }

    @Test void invalidReasonAndEvidenceAndSameTargetAreRejected() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID targetB = createTarget();
        UUID targetA = (UUID) identity(reference).get("jogador_id");
        assertThatThrownBy(() -> transfer.transfer(command(reference, 0, targetB, "consent", "  ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transfer.transfer(command(reference, 0, targetB, "  ", "reason")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transfer.transfer(command(reference, 0, targetB, "x".repeat(256), "reason")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transfer.transfer(command(reference, 0, targetB, "consent", "x".repeat(513))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transfer.transfer(command(reference, 0, targetA, "consent", "reason")))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        assertThat(identity(reference).get("jogador_id")).isEqualTo(targetA);
    }

    @Test void revokedBindingCannotTransferAndDisableAndRevokeRemainDistinctLifecycleActions() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID targetB = createTarget();
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal revocation"));
        assertThatThrownBy(() -> transfer.transfer(command(reference, 1, targetB, "consent", "reason")))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        assertThat(identity(reference)).containsEntry("ownership_status", "REVOKED")
                .containsEntry("ownership_version", 1L);
    }

    @Test void revocationAfterTransferActsOnNewTarget() {
        var reference = createIdentity("VERIFIED", false);
        UUID targetB = createTarget();
        transfer.transfer(command(reference, 1, targetB, "consent", "approved transfer"));
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 2, "revoke transferred binding"));
        assertThat(identity(reference)).containsEntry("jogador_id", targetB)
                .containsEntry("ownership_status", "REVOKED").containsEntry("ownership_version", 3L);
        var revokeEvent = jdbc.queryForMap("""
                SELECT previous_target_jogador_id,new_target_jogador_id
                FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REVOKED'
                """, reference.namespace(), reference.externalId());
        assertThat(revokeEvent).containsEntry("previous_target_jogador_id", targetB)
                .containsEntry("new_target_jogador_id", targetB);
    }

    @Test void historyFailureRollsBackTargetVerificationAndVersion() {
        var reference = createIdentity("VERIFIED", false);
        UUID targetA = (UUID) identity(reference).get("jogador_id");
        UUID targetB = createTarget();
        jdbc.execute("CREATE FUNCTION reject_transfer_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type = 'OWNERSHIP_TRANSFERRED' THEN RAISE EXCEPTION 'intentional transfer history failure'; END IF; RETURN NEW; END; $$");
        jdbc.execute("CREATE TRIGGER reject_transfer_history BEFORE INSERT ON progression_subject_ownership_history FOR EACH ROW EXECUTE FUNCTION reject_transfer_history()");
        try {
            assertThatThrownBy(() -> transfer.transfer(command(reference, 1, targetB, "consent", "reason")))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER reject_transfer_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION reject_transfer_history()");
        }
        assertThat(identity(reference)).containsEntry("jogador_id", targetA)
                .containsEntry("verification_status", "VERIFIED").containsEntry("ownership_version", 1L);
        assertThat(history(reference)).noneMatch(row -> row.get("event_type").equals("OWNERSHIP_TRANSFERRED"));
    }

    @Test void concurrentEquivalentRequestsAppendExactlyOneTransferEvent() throws Exception {
        var reference = createIdentity("VERIFIED", false);
        UUID targetB = createTarget();
        var command = command(reference, 1, targetB, "same-consent", "same handoff");
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> { ready.countDown(); start.await(); transfer.transfer(command); return null; });
            var second = pool.submit(() -> { ready.countDown(); start.await(); transfer.transfer(command); return null; });
            ready.await();
            start.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(identity(reference)).containsEntry("jogador_id", targetB)
                .containsEntry("verification_status", "UNVERIFIED").containsEntry("ownership_version", 2L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_TRANSFERRED'
                """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
    }

    @Test void competingConcurrentTransfersCannotBothCommit() throws Exception {
        var reference = createIdentity("UNVERIFIED", false);
        UUID targetB = createTarget();
        UUID targetC = createTarget();
        UUID winner = null;
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> { ready.countDown(); start.await(); transfer.transfer(command(reference, 0, targetB, "consent-b", "handoff b")); return targetB; });
            var second = pool.submit(() -> { ready.countDown(); start.await(); transfer.transfer(command(reference, 0, targetC, "consent-c", "handoff c")); return targetC; });
            ready.await();
            start.countDown();
            int successes = 0;
            int conflicts = 0;
            try { winner = first.get(); successes++; }
            catch (java.util.concurrent.ExecutionException conflict) {
                assertThat(conflict.getCause()).isInstanceOf(SubjectOwnershipVersionConflictException.class);
                conflicts++;
            }
            try { winner = second.get(); successes++; }
            catch (java.util.concurrent.ExecutionException conflict) {
                assertThat(conflict.getCause()).isInstanceOf(SubjectOwnershipVersionConflictException.class);
                conflicts++;
            }
            assertThat(successes).isEqualTo(1);
            assertThat(conflicts).isEqualTo(1);
            assertThat(winner).isIn(targetB, targetC);
        } finally {
            pool.shutdownNow();
        }
        assertThat(identity(reference)).containsEntry("jogador_id", winner)
                .containsEntry("ownership_version", 1L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_TRANSFERRED'
                """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
    }

    private Map<String, Object> identity(ExternalSubjectReference reference) {
        return jdbc.queryForMap("""
                SELECT id,namespace,external_id,jogador_id,identity_class,ownership_status,
                       verification_status,ownership_version
                FROM progression_subject_identity WHERE namespace=? AND external_id=?
                """, reference.namespace(), reference.externalId());
    }

    private List<Map<String, Object>> history(ExternalSubjectReference reference) {
        return jdbc.queryForList("""
                SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,
                       previous_target_jogador_id,new_target_jogador_id,
                       previous_ownership_status,new_ownership_status,
                       previous_verification_status,new_verification_status,
                       provenance,actor_type,actor_id,evidence_type,evidence_reference,
                       reason,effective_at,recorded_at
                FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? ORDER BY h.aggregate_version
                """, reference.namespace(), reference.externalId());
    }

    private Map<String, Object> transferEvent(ExternalSubjectReference reference, long version) {
        return jdbc.queryForMap("""
                SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,
                       previous_target_jogador_id,new_target_jogador_id,
                       previous_ownership_status,new_ownership_status,
                       previous_verification_status,new_verification_status,
                       provenance,actor_type,actor_id,evidence_type,evidence_reference,
                       reason,effective_at,recorded_at
                FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? AND h.aggregate_version=?
                """, reference.namespace(), reference.externalId(), version);
    }
}
