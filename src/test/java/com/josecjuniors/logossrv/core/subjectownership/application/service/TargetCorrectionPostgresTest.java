package com.josecjuniors.logossrv.core.subjectownership.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.progression.application.service.ProvisionCurrentExternalSubjectIdentityService;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ApproveExternalSubjectTargetCorrectionCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ApproveExternalSubjectTargetCorrectionUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.CorrectExternalSubjectTargetCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.CorrectExternalSubjectTargetUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.DisableExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.DisableExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReactivateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReactivateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.TransferExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.TransferExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.TargetCorrectionAuthorizationStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipTargetCorrectionAuthorizationException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipTargetCorrectionConflictException;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@FreshPostgresIntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=subject-ownership-target-correction-tc-r2")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TargetCorrectionPostgresTest {
    private static final String NAMESPACE = "lifeos";
    @Autowired ApproveExternalSubjectTargetCorrectionUseCase approval;
    @Autowired CorrectExternalSubjectTargetUseCase execution;
    @Autowired RegistrationUseCase registration;
    @Autowired ProvisionCurrentExternalSubjectIdentityService provisioning;
    @Autowired DisableExternalSubjectOwnershipUseCase disable;
    @Autowired VerifyExternalSubjectOwnershipUseCase verify;
    @Autowired InvalidateExternalSubjectOwnershipUseCase invalidate;
    @Autowired ReactivateExternalSubjectOwnershipUseCase reactivate;
    @Autowired RevokeExternalSubjectOwnershipUseCase revoke;
    @Autowired TransferExternalSubjectOwnershipUseCase transfer;
    @Autowired JdbcTemplate jdbc;
    @MockBean SubjectOwnershipOperatorContext operatorContext;
    @SpyBean SubjectOwnershipTargetCorrectionStore correctionStore;
    @SpyBean TargetCorrectionAuthorizationStore authorizationStore;
    private final AtomicReference<String> principal = new AtomicReference<>("tc-reviewer");

    @BeforeEach void setup() {
        reset(correctionStore, authorizationStore, operatorContext);
        when(operatorContext.authorizeForNamespace(NAMESPACE)).thenAnswer(call ->
                new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, principal.get(), NAMESPACE));
        when(operatorContext.authorizeForNamespace("logos-native")).thenAnswer(call ->
                new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, principal.get(), "logos-native"));
    }

    @Test void approvalWritesOnlyAuthorizationAndExactReplayIsIdempotent() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID oldTarget = targetFor(reference);
        UUID correctedTarget = createTarget();
        long historyBefore = countHistory(reference);
        long identitiesBefore = countIdentities(reference);
        UUID pointerBefore = currentIdentity(reference);
        UUID request = UUID.randomUUID();

        var command = approveCommand(reference, predecessor, 0, correctedTarget, request);
        var auth = approval.approve(command);
        assertThat(auth.authorizationId()).isNotNull();
        assertThat(auth.predecessorTargetJogadorId()).isEqualTo(oldTarget);
        assertThat(auth.correctedTargetJogadorId()).isEqualTo(correctedTarget);
        assertThat(auth.correctionBasis()).isEqualTo("wrong initial association");
        assertThat(auth.sourceAssertionReference()).isEqualTo("provisioning-record-17");
        assertThat(auth.authoritativeFactReference()).isEqualTo("authoritative-map-21");
        assertThat(auth.reviewedCaseReference()).isEqualTo("case-42");
        assertThat(auth.reviewerPrincipalId()).isEqualTo("tc-reviewer");
        assertThat(auth.reviewedAt()).isNotNull();
        assertThat(approval.approve(approveCommand(reference, predecessor, 0, correctedTarget, request)))
                .isEqualTo(auth);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_target_correction_authorization WHERE correction_request_id=?",
                Integer.class, request)).isEqualTo(1);
        assertThat(countHistory(reference)).isEqualTo(historyBefore);
        assertThat(countIdentities(reference)).isEqualTo(identitiesBefore);
        assertThat(currentIdentity(reference)).isEqualTo(pointerBefore);
    }

    @Test void approvalChangedPayloadAndReviewerConflict() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID otherTarget = createTarget();
        UUID request = UUID.randomUUID();
        approval.approve(approveCommand(reference, predecessor, 0, target, request));
        assertThatThrownBy(() -> approval.approve(approveCommand(reference, predecessor, 0, otherTarget, request)))
                .isInstanceOf(SubjectOwnershipTargetCorrectionConflictException.class);
        principal.set("different-reviewer");
        assertThatThrownBy(() -> approval.approve(approveCommand(reference, predecessor, 0, target, request)))
                .isInstanceOf(SubjectOwnershipTargetCorrectionConflictException.class);
    }

    @Test void approvalAcceptsEveryCanonicalOwnershipAndVerificationShape() {
        for (String ownership : List.of("ACTIVE", "DISABLED")) {
            for (String verification : List.of("UNVERIFIED", "VERIFIED", "INVALIDATED")) {
                var reference = createIdentity(verification, ownership.equals("DISABLED"));
                UUID predecessor = currentIdentity(reference);
                long version = ownershipVersion(predecessor);
                approval.approve(approveCommand(reference, predecessor, version, createTarget(), UUID.randomUUID()));
            }
        }
    }

    @Test void approvalRejectsRevokedNativeNonRootTransferredAndAlreadyCorrectedBindings() {
        var revoked = createIdentity("UNVERIFIED", false);
        UUID revokedId = currentIdentity(revoked);
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(revoked, 0, "terminal"));
        assertIneligible(revoked, revokedId, 1);

        String email = "native-tc-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "native"));
        UUID nativeExternalId = jdbc.queryForObject("SELECT id FROM app_user WHERE email=?", UUID.class, email);
        var nativeRef = new ExternalSubjectReference("logos-native", nativeExternalId.toString());
        UUID nativeId = currentIdentity(nativeRef);
        assertThatThrownBy(() -> approval.approve(approveCommand(nativeRef, nativeId, 0,
                createTarget(), UUID.randomUUID()))).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);

        var transferred = createIdentity("UNVERIFIED", false);
        UUID transferredId = currentIdentity(transferred);
        transfer.transfer(new TransferExternalSubjectOwnershipCommand(transferred, 0,
                createTarget(), "bilateral-consent", "legitimate handoff"));
        assertIneligible(transferred, transferredId, 1);

        var corrected = executeCorrection(createIdentity("UNVERIFIED", false), "UNVERIFIED", false);
        var childRef = corrected.reference();
        UUID childId = corrected.successorIdentityId();
        assertThatThrownBy(() -> approval.approve(approveCommand(childRef, childId, 0,
                createTarget(), UUID.randomUUID()))).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        assertThatThrownBy(() -> approval.approve(approveCommand(childRef, corrected.predecessorIdentityId(), 0,
                createTarget(), UUID.randomUUID()))).isInstanceOf(SubjectOwnershipTargetCorrectionConflictException.class);
    }

    @Test void approvalRejectsWrongVersionMissingOrSameTargetAndBlankEvidence() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        assertThatThrownBy(() -> approval.approve(approveCommand(reference, predecessor, 1, target, UUID.randomUUID())))
                .isInstanceOf(SubjectOwnershipTargetCorrectionConflictException.class);
        assertThatThrownBy(() -> approval.approve(approveCommand(reference, predecessor, 0, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        assertThatThrownBy(() -> approval.approve(approveCommand(reference, predecessor, 0,
                targetFor(reference), UUID.randomUUID()))).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        for (int index = 0; index < 4; index++) {
            var blank = new ApproveExternalSubjectTargetCorrectionCommand(reference, predecessor, 0, target,
                    UUID.randomUUID(), index == 0 ? " " : "basis", index == 1 ? " " : "source",
                    index == 2 ? " " : "fact", index == 3 ? " " : "case");
            assertThatThrownBy(() -> approval.approve(blank)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void executionCreatesModelBWithExactHistoryAndMovesOnlyCurrentPointer() {
        for (String verification : List.of("UNVERIFIED", "VERIFIED", "INVALIDATED")) {
            for (boolean disabled : List.of(false, true)) {
                var reference = createIdentity(verification, disabled);
                UUID predecessor = currentIdentity(reference);
                UUID targetBefore = targetFor(reference);
                long version = ownershipVersion(predecessor);
                var before = identityJson(predecessor);
                UUID target = createTarget();
                UUID request = UUID.randomUUID();
                var auth = approval.approve(approveCommand(reference, predecessor, version, target, request));
                principal.set("tc-executor");
                var command = executeCommand(reference, predecessor, version, target, request, auth.authorizationId(), "  proven inception error  ");
                var result = execution.correct(command);

                String expectedVerification = verification.equals("VERIFIED") ? "UNVERIFIED" : verification;
                assertThat(result.successorIdentityId()).isNotEqualTo(predecessor);
                assertThat(result.reference()).isEqualTo(reference);
                assertThat(result.correctedTargetJogadorId()).isEqualTo(target);
                assertThat(result.ownershipStatus()).hasToString("DISABLED");
                assertThat(result.verificationStatus()).hasToString(expectedVerification);
                assertThat(result.ownershipVersion()).isZero();
                assertThat(currentIdentity(reference)).isEqualTo(result.successorIdentityId());
                assertThat(identityJson(predecessor)).isEqualTo(before);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE predecessor_identity_id=?",
                        Integer.class, predecessor)).isEqualTo(1);

                var event = jdbc.queryForMap("""
                        SELECT * FROM progression_subject_ownership_history
                        WHERE event_type='OWNERSHIP_TARGET_CORRECTED' AND correction_request_id=?
                        """, request);
                assertThat(event).containsEntry("identity_id", result.successorIdentityId())
                        .containsEntry("aggregate_version", 0L)
                        .containsEntry("previous_identity_class", "EXTERNAL")
                        .containsEntry("new_identity_class", "EXTERNAL")
                        .containsEntry("previous_target_jogador_id", targetBefore)
                        .containsEntry("new_target_jogador_id", target)
                        .containsEntry("previous_ownership_status", disabled ? "DISABLED" : "ACTIVE")
                        .containsEntry("new_ownership_status", "DISABLED")
                        .containsEntry("previous_verification_status", verification)
                        .containsEntry("new_verification_status", expectedVerification)
                        .containsEntry("provenance", "LOGOS_OPERATOR_ACTION")
                        .containsEntry("actor_type", "WORKLOAD_OPERATOR")
                        .containsEntry("actor_id", "tc-executor")
                        .containsEntry("evidence_type", "ADMINISTRATIVE_TARGET_CORRECTION_AUTHORIZATION")
                        .containsEntry("evidence_reference", auth.authorizationId().toString())
                        .containsEntry("reason", "proven inception error")
                        .containsEntry("predecessor_identity_id", predecessor)
                        .containsEntry("predecessor_ownership_version", version)
                        .containsEntry("target_correction_authorization_id", auth.authorizationId());
                assertThat(event.get("effective_at")).isNotNull();
                assertThat(event.get("recorded_at")).isNotNull();
                assertThat(execution.correct(command)).isEqualTo(result);
                principal.set("tc-reviewer");
            }
        }
    }

    @Test void exactReplayUsesImmutableEventAfterSuccessorLifecycleAndRejectsChangedPayloadOrExecutor() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID request = UUID.randomUUID();
        var auth = approval.approve(approveCommand(reference, predecessor, 0, target, request));
        principal.set("tc-executor");
        var command = executeCommand(reference, predecessor, 0, target, request, auth.authorizationId(), "original reason");
        var result = execution.correct(command);
        reactivate.reactivate(new ReactivateExternalSubjectOwnershipCommand(reference, 0, "activate successor"));
        verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, 1,
                "verify-case", "verify-source", "verify successor"));
        assertThat(execution.correct(command)).isEqualTo(result);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE predecessor_identity_id=?",
                Integer.class, predecessor)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE correction_request_id=?",
                Integer.class, request)).isEqualTo(1);
        assertThatThrownBy(() -> execution.correct(executeCommand(reference, predecessor, 0, target, request,
                auth.authorizationId(), "changed reason"))).isInstanceOf(SubjectOwnershipTargetCorrectionConflictException.class);
        principal.set("different-executor");
        assertThatThrownBy(() -> execution.correct(command)).isInstanceOf(SubjectOwnershipTargetCorrectionConflictException.class);
    }

    @Test void exactReplaySurvivesSuccessorTransferAndRevocation() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID request = UUID.randomUUID();
        var auth = approval.approve(approveCommand(reference, predecessor, 0, target, request));
        principal.set("tc-executor");
        var command = executeCommand(reference, predecessor, 0, target, request, auth.authorizationId(), "original reason");
        var result = execution.correct(command);

        reactivate.reactivate(new ReactivateExternalSubjectOwnershipCommand(reference, 0, "activate successor"));
        verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, 1,
                "verify-case", "verify-source", "verify successor"));
        transfer.transfer(new TransferExternalSubjectOwnershipCommand(reference, 2,
                createTarget(), "bilateral-consent", "later legitimate handoff"));
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 3, "later terminal revocation"));

        assertThat(execution.correct(command)).isEqualTo(result);
        assertThat(currentIdentity(reference)).isEqualTo(result.successorIdentityId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE predecessor_identity_id=?",
                Integer.class, predecessor)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE correction_request_id=?",
                Integer.class, request)).isEqualTo(1);
    }

    @Test void authorizationMismatchIsCoarseAndReviewerCannotExecuteOwnApproval() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID request = UUID.randomUUID();
        var auth = approval.approve(approveCommand(reference, predecessor, 0, target, request));
        assertThatThrownBy(() -> execution.correct(executeCommand(reference, predecessor, 0, target, request,
                UUID.randomUUID(), "reason"))).isInstanceOf(SubjectOwnershipTargetCorrectionAuthorizationException.class)
                .hasMessage("Target correction authorization is unavailable or does not match");
        assertThatThrownBy(() -> execution.correct(executeCommand(reference, predecessor, 0, target, request,
                auth.authorizationId(), "reason"))).isInstanceOf(SubjectOwnershipTargetCorrectionAuthorizationException.class)
                .hasMessage("Target correction authorization is unavailable or does not match");
    }

    @Test void authorizationBeforeDisclosureAndStaleApprovalAreRejected() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(operatorContext).authorizeForNamespace(NAMESPACE);
        var missing = new ExternalSubjectReference(NAMESPACE, "secret-absent");
        assertThatThrownBy(() -> approval.approve(approveCommand(missing, UUID.randomUUID(), 0,
                UUID.randomUUID(), UUID.randomUUID()))).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(correctionStore, authorizationStore);

        reset(operatorContext);
        when(operatorContext.authorizeForNamespace(NAMESPACE)).thenAnswer(call ->
                new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, principal.get(), NAMESPACE));
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID request = UUID.randomUUID();
        var auth = approval.approve(approveCommand(reference, predecessor, 0, target, request));
        verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, 0,
                "verify-case", "verify-source", "change predecessor after approval"));
        principal.set("tc-executor");
        assertThatThrownBy(() -> execution.correct(executeCommand(reference, predecessor, 0, target, request,
                auth.authorizationId(), "reason"))).isInstanceOf(SubjectOwnershipTargetCorrectionConflictException.class);
        assertThat(currentIdentity(reference)).isEqualTo(predecessor);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE predecessor_identity_id=?",
                Integer.class, predecessor)).isZero();
        assertThat(authorizationStore.findAuthorizationById(auth.authorizationId())).isPresent();
    }

    @Test void concurrentSameRequestCreatesOneSuccessorAndOneEvent() throws Exception {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID request = UUID.randomUUID();
        var auth = approval.approve(approveCommand(reference, predecessor, 0, target, request));
        var command = executeCommand(reference, predecessor, 0, target, request, auth.authorizationId(), "same request");
        principal.set("tc-executor");
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> { start.await(); return execution.correct(command); });
            var b = pool.submit(() -> { start.await(); return execution.correct(command); });
            start.countDown();
            var first = a.get();
            var second = b.get();
            assertThat(second).isEqualTo(first);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE predecessor_identity_id=?",
                Integer.class, predecessor)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE correction_request_id=?",
                Integer.class, request)).isEqualTo(1);
    }

    @Test void concurrentDistinctRequestsAgainstOnePredecessorHaveAtMostOneWinner() throws Exception {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID targetA = createTarget();
        UUID targetB = createTarget();
        UUID requestA = UUID.randomUUID();
        UUID requestB = UUID.randomUUID();
        var authA = approval.approve(approveCommand(reference, predecessor, 0, targetA, requestA));
        var authB = approval.approve(approveCommand(reference, predecessor, 0, targetB, requestB));
        var commandA = executeCommand(reference, predecessor, 0, targetA, requestA, authA.authorizationId(), "race A");
        var commandB = executeCommand(reference, predecessor, 0, targetB, requestB, authB.authorizationId(), "race B");
        principal.set("tc-executor");
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> { start.await(); return execution.correct(commandA); });
            var b = pool.submit(() -> { start.await(); return execution.correct(commandB); });
            start.countDown();
            int succeeded = 0;
            for (var future : List.of(a, b)) {
                try { future.get(); succeeded++; }
                catch (java.util.concurrent.ExecutionException conflict) { /* loser is expected */ }
            }
            assertThat(succeeded).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE predecessor_identity_id=?",
                Integer.class, predecessor)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE event_type='OWNERSHIP_TARGET_CORRECTED' AND predecessor_identity_id=?",
                Integer.class, predecessor)).isEqualTo(1);
    }

    @Test void pointerCasZeroRollsBackSuccessorAndHistoryButKeepsAuthorization() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID request = UUID.randomUUID();
        var auth = approval.approve(approveCommand(reference, predecessor, 0, target, request));
        principal.set("tc-executor");
        doReturn(0).when(correctionStore).compareAndSetCurrentPointerForTargetCorrection(eq(reference),
                eq(predecessor), any(UUID.class));
        assertThatThrownBy(() -> execution.correct(executeCommand(reference, predecessor, 0, target, request,
                auth.authorizationId(), "cas failure"))).isInstanceOf(SubjectOwnershipTargetCorrectionConflictException.class);
        reset(correctionStore);
        assertThat(currentIdentity(reference)).isEqualTo(predecessor);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE predecessor_identity_id=?",
                Integer.class, predecessor)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE correction_request_id=?",
                Integer.class, request)).isZero();
        assertThat(authorizationStore.findAuthorizationById(auth.authorizationId())).isPresent();
    }

    @Test void successorInsertFailureLeavesNoHistoryOrPointerChangeAndKeepsAuthorization() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID request = UUID.randomUUID();
        var auth = approval.approve(approveCommand(reference, predecessor, 0, target, request));
        principal.set("tc-executor");
        doThrow(new DataAccessException("intentional successor insert failure") { })
                .when(correctionStore).insertSuccessor(any(), any(UUID.class), any(UUID.class), any());

        assertThatThrownBy(() -> execution.correct(executeCommand(reference, predecessor, 0, target, request,
                auth.authorizationId(), "successor failure"))).isInstanceOf(DataAccessException.class);
        reset(correctionStore);
        assertThat(currentIdentity(reference)).isEqualTo(predecessor);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE predecessor_identity_id=?",
                Integer.class, predecessor)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE correction_request_id=?",
                Integer.class, request)).isZero();
        assertThat(authorizationStore.findAuthorizationById(auth.authorizationId())).isPresent();
    }

    @Test void historyConstraintFailureRollsBackSuccessorAndPointer() {
        var reference = createIdentity("UNVERIFIED", false);
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID request = UUID.randomUUID();
        var auth = approval.approve(approveCommand(reference, predecessor, 0, target, request));
        principal.set("tc-executor");
        jdbc.execute("""
                CREATE FUNCTION reject_test_target_correction_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'intentional target correction history failure'; END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER trg_reject_test_target_correction_history
                BEFORE INSERT ON progression_subject_ownership_history
                FOR EACH ROW WHEN (NEW.event_type = 'OWNERSHIP_TARGET_CORRECTED')
                EXECUTE FUNCTION reject_test_target_correction_history()
                """);
        try {
            assertThatThrownBy(() -> execution.correct(executeCommand(reference, predecessor, 0, target, request,
                    auth.authorizationId(), "history failure"))).isInstanceOf(DataAccessException.class);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS trg_reject_test_target_correction_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION IF EXISTS reject_test_target_correction_history()");
        }
        assertThat(currentIdentity(reference)).isEqualTo(predecessor);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE predecessor_identity_id=?",
                Integer.class, predecessor)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE correction_request_id=?",
                Integer.class, request)).isZero();
        assertThat(authorizationStore.findAuthorizationById(auth.authorizationId())).isPresent();
    }

    private com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionResult
            executeCorrection(ExternalSubjectReference reference, String verification, boolean disabled) {
        UUID predecessor = currentIdentity(reference);
        long version = ownershipVersion(predecessor);
        UUID target = createTarget();
        UUID request = UUID.randomUUID();
        var auth = approval.approve(approveCommand(reference, predecessor, version, target, request));
        principal.set("tc-executor");
        var result = execution.correct(executeCommand(reference, predecessor, version, target,
                request, auth.authorizationId(), "correction"));
        principal.set("tc-reviewer");
        return result;
    }

    private void assertIneligible(ExternalSubjectReference reference, UUID predecessor, long version) {
        assertThatThrownBy(() -> approval.approve(approveCommand(reference, predecessor, version,
                createTarget(), UUID.randomUUID()))).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
    }

    private ExternalSubjectReference createIdentity(String verification, boolean disabled) {
        String email = "tc-source-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "TC-source-" + UUID.randomUUID()));
        var reference = new ExternalSubjectReference(NAMESPACE, "external-" + UUID.randomUUID());
        provisioning.provision(email, reference);
        long version = 0;
        if (verification.equals("VERIFIED") || verification.equals("INVALIDATED")) {
            verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, version,
                    "initial-review", "verification-source", "verify before correction"));
            version++;
        }
        if (verification.equals("INVALIDATED")) {
            invalidate.invalidate(new InvalidateExternalSubjectOwnershipCommand(reference, version,
                    "invalid-review", "invalid-source", "invalidate before correction"));
            version++;
        }
        if (disabled) disable.disable(new DisableExternalSubjectOwnershipCommand(reference, version,
                "disable before correction"));
        principal.set("tc-reviewer");
        return reference;
    }

    private UUID createTarget() {
        String email = "tc-target-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "TC-target-" + UUID.randomUUID()));
        return jdbc.queryForObject("SELECT id FROM jogador WHERE user_id=(SELECT id FROM app_user WHERE email=?)",
                UUID.class, email);
    }

    private ApproveExternalSubjectTargetCorrectionCommand approveCommand(ExternalSubjectReference reference,
            UUID predecessor, long version, UUID target, UUID request) {
        return new ApproveExternalSubjectTargetCorrectionCommand(reference, predecessor, version, target,
                request, "  wrong initial association  ", "  provisioning-record-17  ",
                "  authoritative-map-21  ", "  case-42  ");
    }

    private CorrectExternalSubjectTargetCommand executeCommand(ExternalSubjectReference reference,
            UUID predecessor, long version, UUID target, UUID request, UUID authorization, String reason) {
        return new CorrectExternalSubjectTargetCommand(reference, predecessor, version, target,
                request, authorization, reason);
    }

    private UUID currentIdentity(ExternalSubjectReference reference) {
        return jdbc.queryForObject("SELECT current_identity_id FROM progression_subject_current_binding WHERE namespace=? AND external_id=?",
                UUID.class, reference.namespace(), reference.externalId());
    }

    private UUID targetFor(ExternalSubjectReference reference) {
        return jdbc.queryForObject("SELECT jogador_id FROM progression_subject_identity WHERE id=?", UUID.class,
                currentIdentity(reference));
    }

    private long ownershipVersion(UUID identityId) {
        return jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE id=?", Long.class,
                identityId);
    }

    private long countHistory(ExternalSubjectReference reference) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=?
                """, Long.class, reference.namespace(), reference.externalId());
    }

    private long countIdentities(ExternalSubjectReference reference) {
        return jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace=? AND external_id=?",
                Long.class, reference.namespace(), reference.externalId());
    }

    private String identityJson(UUID identityId) {
        return jdbc.queryForObject("SELECT to_jsonb(identity)::text FROM progression_subject_identity identity WHERE id=?",
                String.class, identityId);
    }
}
