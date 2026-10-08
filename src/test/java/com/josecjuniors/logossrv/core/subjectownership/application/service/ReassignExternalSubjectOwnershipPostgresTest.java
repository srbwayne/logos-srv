package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.progression.application.service.ProvisionCurrentExternalSubjectIdentityService;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ApproveExternalSubjectReassignmentCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ApproveExternalSubjectReassignmentUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReassignExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReassignExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReactivateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReactivateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.TransferExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.TransferExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipReassignmentConflictException;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;

@FreshPostgresIntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=subject-ownership-reassignment-r2")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReassignExternalSubjectOwnershipPostgresTest {
    private static final String NAMESPACE = "lifeos";
    @Autowired ApproveExternalSubjectReassignmentUseCase approval;
    @Autowired ReassignExternalSubjectOwnershipUseCase reassignment;
    @Autowired RevokeExternalSubjectOwnershipUseCase revoke;
    @Autowired ReactivateExternalSubjectOwnershipUseCase reactivate;
    @Autowired TransferExternalSubjectOwnershipUseCase transfer;
    @Autowired VerifyExternalSubjectOwnershipUseCase verify;
    @Autowired InvalidateExternalSubjectOwnershipUseCase invalidate;
    @Autowired RegistrationUseCase registration;
    @Autowired ProvisionCurrentExternalSubjectIdentityService provisioning;
    @Autowired JdbcTemplate jdbc;
    @MockBean SubjectOwnershipOperatorContext operatorContext;
    @SpyBean SubjectOwnershipReassignmentStore reassignmentStore;
    private final AtomicReference<String> principal = new AtomicReference<>("reviewer-r2");

    @BeforeEach void setup() {
        reset(reassignmentStore);
        when(operatorContext.authorizeForNamespace(NAMESPACE)).thenAnswer(invocation ->
                new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, principal.get(), NAMESPACE));
    }

    @Test void historyInsertFailureRollsBackSuccessorAndLeavesPointerUnchanged() {
        var reference = createIdentity();
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal recovery predecessor"));
        UUID request = UUID.randomUUID();
        var auth = approval.approve(new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1,
                target, request, "recover", "case-history-failure"));
        principal.set("executor-history-failure");
        jdbc.execute("""
                CREATE FUNCTION reject_test_reassignment_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'intentional reassignment history failure'; END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER trg_reject_test_reassignment_history
                BEFORE INSERT ON progression_subject_ownership_history
                FOR EACH ROW WHEN (NEW.event_type = 'OWNERSHIP_REASSIGNED')
                EXECUTE FUNCTION reject_test_reassignment_history()
                """);
        try {
            var command = new ReassignExternalSubjectOwnershipCommand(reference, predecessor, 1, target,
                    request, auth.evidenceReference(), "replacement");
            assertThatThrownBy(() -> reassignment.reassign(command)).isInstanceOf(DataAccessException.class);
            assertThat(currentIdentity(reference)).isEqualTo(predecessor);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace=? AND external_id=?",
                    Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE event_type='OWNERSHIP_REASSIGNED' AND reassignment_request_id=?",
                    Integer.class, request)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS trg_reject_test_reassignment_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION IF EXISTS reject_test_reassignment_history()");
        }
    }

    @Test void pointerCompareAndSwitchFailureRollsBackSuccessorAndHistory() {
        var reference = createIdentity();
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal recovery predecessor"));
        UUID request = UUID.randomUUID();
        var auth = approval.approve(new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1,
                target, request, "recover", "case-pointer-failure"));
        principal.set("executor-pointer-failure");
        // Match any server-generated successor ID while keeping the adapter's write path intact.
        org.mockito.Mockito.doAnswer(invocation -> 0).when(reassignmentStore)
                .switchCurrentPointer(org.mockito.ArgumentMatchers.eq(reference),
                        org.mockito.ArgumentMatchers.eq(predecessor), org.mockito.ArgumentMatchers.any(UUID.class));
        var command = new ReassignExternalSubjectOwnershipCommand(reference, predecessor, 1, target,
                request, auth.evidenceReference(), "replacement");
        assertThatThrownBy(() -> reassignment.reassign(command))
                .isInstanceOf(SubjectOwnershipReassignmentConflictException.class);
        reset(reassignmentStore);
        assertThat(currentIdentity(reference)).isEqualTo(predecessor);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace=? AND external_id=?",
                Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE event_type='OWNERSHIP_REASSIGNED' AND reassignment_request_id=?",
                Integer.class, request)).isZero();
    }

    @Test void approvesRevokedCurrentPredecessorAndExecutesAtomicSuccessorThenExactReplay() {
        var reference = createIdentity();
        UUID predecessor = currentIdentity(reference);
        UUID oldTarget = targetFor(reference);
        UUID newTarget = createTarget();
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal recovery predecessor"));
        UUID requestId = UUID.randomUUID();
        var command = new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1, newTarget,
                requestId, "  compromised account  ", "  case-42  ");

        var authorized = approval.approve(command);
        assertThat(authorized.authorizationId()).isNotNull();
        assertThat(authorized.recoveryBasis()).isEqualTo("compromised account");
        assertThat(authorized.reviewedCaseReference()).isEqualTo("case-42");
        assertThat(authorized.predecessorTargetJogadorId()).isEqualTo(oldTarget);
        assertThat(authorized.reviewedAt()).isNotNull();
        assertThat(approval.approve(command)).isEqualTo(authorized);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_reassignment_authorization WHERE reassignment_request_id=?",
                Integer.class, requestId)).isEqualTo(1);

        principal.set("executor-r2");
        var reassignmentCommand = new ReassignExternalSubjectOwnershipCommand(reference, predecessor, 1, newTarget,
                requestId, authorized.evidenceReference(), "  replaced compromised target  ");
        var result = reassignment.reassign(reassignmentCommand);
        assertThat(result.successorIdentityId()).isNotEqualTo(predecessor);
        assertThat(result.targetJogadorId()).isEqualTo(newTarget);
        assertThat(result.ownershipStatus()).hasToString("DISABLED");
        assertThat(result.verificationStatus()).hasToString("UNVERIFIED");
        assertThat(result.ownershipVersion()).isZero();
        assertThat(jdbc.queryForObject("SELECT current_identity_id FROM progression_subject_current_binding WHERE namespace=? AND external_id=?",
                UUID.class, reference.namespace(), reference.externalId())).isEqualTo(result.successorIdentityId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE event_type='OWNERSHIP_REASSIGNED' AND reassignment_request_id=?",
                Integer.class, requestId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ownership_status FROM progression_subject_identity WHERE id=?",
                String.class, predecessor)).isEqualTo("REVOKED");

        reactivate.reactivate(new ReactivateExternalSubjectOwnershipCommand(reference, 0, "activate successor"));
        var replay = reassignment.reassign(reassignmentCommand);
        assertThat(replay).isEqualTo(result);
        assertThat(jdbc.queryForObject("SELECT ownership_status FROM progression_subject_identity WHERE id=?",
                String.class, result.successorIdentityId())).isEqualTo("ACTIVE");

        verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, 1,
                "follow-up-verification", "follow-up-case", "verify reassigned successor"));
        assertThat(reassignment.reassign(reassignmentCommand)).isEqualTo(result);

        UUID transferredTarget = createTarget();
        transfer.transfer(new TransferExternalSubjectOwnershipCommand(reference, 2, transferredTarget,
                "bilateral-consent", "subsequent legitimate handoff"));
        assertThat(reassignment.reassign(reassignmentCommand)).isEqualTo(result);

        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 3, "terminalize second binding"));
        assertThat(reassignment.reassign(reassignmentCommand)).isEqualTo(result);
        principal.set("reviewer-r2");
        UUID nextTarget = createTarget();
        UUID nextRequest = UUID.randomUUID();
        var nextApproval = approval.approve(new ApproveExternalSubjectReassignmentCommand(reference,
                result.successorIdentityId(), 4, nextTarget, nextRequest, "second recovery", "case-43"));
        principal.set("executor-r2");
        reassignment.reassign(new ReassignExternalSubjectOwnershipCommand(reference, result.successorIdentityId(), 4,
                nextTarget, nextRequest, nextApproval.evidenceReference(), "second recovery operation"));
        assertThat(reassignment.reassign(reassignmentCommand)).isEqualTo(result);
    }

    @Test void preservesCanonicalVerificationMappingForAllEligiblePredecessorStates() {
        for (String state : new String[] {"UNVERIFIED", "VERIFIED", "INVALIDATED"}) {
            var reference = createIdentity();
            UUID predecessor = currentIdentity(reference);
            UUID target = createTarget();
            long version = 0;
            if (state.equals("VERIFIED") || state.equals("INVALIDATED")) {
                verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, version,
                        "initial-review", "verification-case", "verified before recovery"));
                version++;
            }
            if (state.equals("INVALIDATED")) {
                invalidate.invalidate(new InvalidateExternalSubjectOwnershipCommand(reference, version,
                        "withdrawal-review", "withdrawal-case", "verification invalidated"));
                version++;
            }
            revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, version, "terminal recovery predecessor"));
            version++;
            UUID request = UUID.randomUUID();
            var auth = approval.approve(new ApproveExternalSubjectReassignmentCommand(reference, predecessor,
                    version, target, request, "recovery", "case-" + state));
            principal.set("executor-map-" + state);
            var result = reassignment.reassign(new ReassignExternalSubjectOwnershipCommand(reference, predecessor,
                    version, target, request, auth.evidenceReference(), "recovery " + state));
            String expected = state.equals("VERIFIED") ? "UNVERIFIED" : state;
            assertThat(result.verificationStatus().name()).isEqualTo(expected);
            principal.set("reviewer-r2");
        }
    }

    @Test void approvalRequestPayloadOrReviewerChangesConflict() {
        var reference = createIdentity();
        UUID predecessor = currentIdentity(reference);
        UUID newTarget = createTarget();
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal recovery predecessor"));
        UUID request = UUID.randomUUID();
        var original = new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1, newTarget,
                request, "recover", "case");
        approval.approve(original);
        assertThatThrownBy(() -> approval.approve(new ApproveExternalSubjectReassignmentCommand(reference,
                predecessor, 1, newTarget, request, "different basis", "case")))
                .isInstanceOf(SubjectOwnershipReassignmentConflictException.class);
        principal.set("different-reviewer");
        assertThatThrownBy(() -> approval.approve(original))
                .isInstanceOf(SubjectOwnershipReassignmentConflictException.class);
    }

    @Test void sameRequestDifferentExecutorCannotReplay() {
        var reference = createIdentity();
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal recovery predecessor"));
        UUID request = UUID.randomUUID();
        var auth = approval.approve(new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1,
                target, request, "recover", "case"));
        principal.set("executor-one");
        var cmd = new ReassignExternalSubjectOwnershipCommand(reference, predecessor, 1, target, request,
                auth.evidenceReference(), "replacement");
        reassignment.reassign(cmd);
        assertThatThrownBy(() -> reassignment.reassign(new ReassignExternalSubjectOwnershipCommand(reference,
                predecessor, 1, target, request, auth.evidenceReference(), "different reason")))
                .isInstanceOf(SubjectOwnershipReassignmentConflictException.class);
        principal.set("executor-two");
        assertThatThrownBy(() -> reassignment.reassign(cmd))
                .isInstanceOf(SubjectOwnershipReassignmentConflictException.class);
    }

    @Test void concurrentEquivalentApprovalReturnsOneImmutableAuthorization() throws Exception {
        var reference = createIdentity();
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal recovery predecessor"));
        var command = new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1, target,
                UUID.randomUUID(), "recover", "case");
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> approval.approve(command));
            Future<?> second = pool.submit(() -> approval.approve(command));
            var a = (com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentAuthorization) first.get();
            var b = (com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentAuthorization) second.get();
            assertThat(a).isEqualTo(b);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_reassignment_authorization WHERE reassignment_request_id=?",
                Integer.class, command.reassignmentRequestId())).isEqualTo(1);
    }

    @Test void equivalentConcurrentReassignmentProducesOneSuccessorAndEvent() throws Exception {
        var reference = createIdentity();
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal recovery predecessor"));
        UUID request = UUID.randomUUID();
        var auth = approval.approve(new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1,
                target, request, "recover", "case"));
        principal.set("executor-concurrent");
        var command = new ReassignExternalSubjectOwnershipCommand(reference, predecessor, 1, target, request,
                auth.evidenceReference(), "replacement");
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> reassignment.reassign(command));
            Future<?> second = pool.submit(() -> reassignment.reassign(command));
            var a = (com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentResult) first.get();
            var b = (com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentResult) second.get();
            assertThat(a).isEqualTo(b);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace=? AND external_id=?",
                Integer.class, reference.namespace(), reference.externalId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE event_type='OWNERSHIP_REASSIGNED' AND reassignment_request_id=?",
                Integer.class, request)).isEqualTo(1);
    }

    @Test void concurrentSameRequestWithDifferentPayloadHasOneWinnerAndOneConflict() throws Exception {
        var reference = createIdentity();
        UUID predecessor = currentIdentity(reference);
        UUID target = createTarget();
        UUID alternateTarget = createTarget();
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal recovery predecessor"));
        UUID request = UUID.randomUUID();
        var auth = approval.approve(new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1,
                target, request, "recover", "case"));
        principal.set("executor-race");
        var accepted = new ReassignExternalSubjectOwnershipCommand(reference, predecessor, 1, target, request,
                auth.evidenceReference(), "payload one");
        var changed = new ReassignExternalSubjectOwnershipCommand(reference, predecessor, 1, alternateTarget, request,
                auth.evidenceReference(), "payload two");
        var outcomes = race(() -> reassignment.reassign(accepted), () -> reassignment.reassign(changed));
        assertOneSuccessOneConflict(outcomes);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace=? AND external_id=?",
                Integer.class, reference.namespace(), reference.externalId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE event_type='OWNERSHIP_REASSIGNED' AND reassignment_request_id=?",
                Integer.class, request)).isEqualTo(1);
    }

    @Test void distinctRequestsCompetingForOnePredecessorHaveOneWinner() throws Exception {
        for (boolean sameTarget : new boolean[] {true, false}) {
            principal.set("reviewer-r2");
            var reference = createIdentity();
            UUID predecessor = currentIdentity(reference);
            UUID targetA = createTarget();
            UUID targetB = sameTarget ? targetA : createTarget();
            revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "terminal recovery predecessor"));
            UUID requestA = UUID.randomUUID();
            UUID requestB = UUID.randomUUID();
            var authA = approval.approve(new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1,
                    targetA, requestA, "recover A", "case A"));
            var authB = approval.approve(new ApproveExternalSubjectReassignmentCommand(reference, predecessor, 1,
                    targetB, requestB, "recover B", "case B"));
            principal.set("executor-competing");
            var commandA = new ReassignExternalSubjectOwnershipCommand(reference, predecessor, 1, targetA, requestA,
                    authA.evidenceReference(), "competing A");
            var commandB = new ReassignExternalSubjectOwnershipCommand(reference, predecessor, 1, targetB, requestB,
                    authB.evidenceReference(), "competing B");
            assertOneSuccessOneConflict(race(() -> reassignment.reassign(commandA),
                    () -> reassignment.reassign(commandB)));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace=? AND external_id=?",
                    Integer.class, reference.namespace(), reference.externalId())).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history WHERE event_type='OWNERSHIP_REASSIGNED' AND identity_id IN (SELECT id FROM progression_subject_identity WHERE namespace=? AND external_id=?)",
                    Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
        }
    }

    private Object[] race(java.util.concurrent.Callable<?> first, java.util.concurrent.Callable<?> second)
            throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> a = pool.submit(() -> outcome(first));
            Future<Object> b = pool.submit(() -> outcome(second));
            return new Object[] {a.get(), b.get()};
        } finally {
            pool.shutdownNow();
        }
    }

    private static Object outcome(java.util.concurrent.Callable<?> call) {
        try { return call.call(); }
        catch (RuntimeException failure) { return failure; }
        catch (Exception failure) { throw new RuntimeException(failure); }
    }

    private static void assertOneSuccessOneConflict(Object[] outcomes) {
        long results = java.util.Arrays.stream(outcomes)
                .filter(com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentResult.class::isInstance)
                .count();
        long conflicts = java.util.Arrays.stream(outcomes)
                .filter(SubjectOwnershipReassignmentConflictException.class::isInstance).count();
        assertThat(results).as("concurrent outcomes: %s", java.util.Arrays.toString(outcomes)).isEqualTo(1);
        assertThat(conflicts).as("concurrent outcomes: %s", java.util.Arrays.toString(outcomes)).isEqualTo(1);
    }

    private ExternalSubjectReference createIdentity() {
        String email = "reassign-r2-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "Reassignment " + UUID.randomUUID()));
        var reference = new ExternalSubjectReference(NAMESPACE, "external-" + UUID.randomUUID());
        provisioning.provision(email, reference);
        return reference;
    }

    private UUID createTarget() {
        String email = "reassign-target-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "Reassignment target " + UUID.randomUUID()));
        return jdbc.queryForObject("SELECT id FROM jogador WHERE user_id=(SELECT id FROM app_user WHERE email=?)",
                UUID.class, email);
    }

    private UUID currentIdentity(ExternalSubjectReference reference) {
        return jdbc.queryForObject("SELECT current_identity_id FROM progression_subject_current_binding WHERE namespace=? AND external_id=?",
                UUID.class, reference.namespace(), reference.externalId());
    }

    private UUID targetFor(ExternalSubjectReference reference) {
        return jdbc.queryForObject("SELECT jogador_id FROM progression_subject_identity WHERE id=?",
                UUID.class, currentIdentity(reference));
    }
}
