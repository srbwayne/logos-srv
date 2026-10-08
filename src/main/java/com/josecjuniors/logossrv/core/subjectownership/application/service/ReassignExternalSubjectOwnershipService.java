package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReassignExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReassignExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentAuthorization;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentResult;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipReassignmentAuthorizationException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipReassignmentConflictException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.IdentityClass;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipStatus;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReassignExternalSubjectOwnershipService implements ReassignExternalSubjectOwnershipUseCase {
    private static final String EVIDENCE_TYPE = "ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION";
    private final SubjectOwnershipOperatorContext operators;
    private final SubjectOwnershipReassignmentStore store;

    public ReassignExternalSubjectOwnershipService(SubjectOwnershipOperatorContext operators,
            SubjectOwnershipReassignmentStore store) {
        this.operators = operators;
        this.store = store;
    }

    @Override
    @Transactional
    public SubjectOwnershipReassignmentResult reassign(ReassignExternalSubjectOwnershipCommand command) {
        var executor = operators.authorizeForNamespace(command.reference().namespace());
        if (!executor.namespace().equals(command.reference().namespace()))
            throw new SecurityException("Namespace authorization mismatch");
        if (command.predecessorIdentityId() == null || command.newTargetJogadorId() == null
                || command.reassignmentRequestId() == null || command.expectedPredecessorOwnershipVersion() < 0)
            throw new IllegalArgumentException("required reassignment fields are invalid");
        if (command.reason() == null) throw new IllegalArgumentException("reason is required");
        String normalizedReason = command.reason().trim();
        if (normalizedReason.isEmpty() || normalizedReason.length() > 512)
            throw new IllegalArgumentException("reason must be nonblank and at most 512 characters");
        var authorizationId = command.authorizationId();
        var initial = store.findCompletedReassignmentByRequestId(command.reassignmentRequestId());
        if (initial.isPresent()) return replayWithAuthorization(initial.get(), command, executor.principalId());

        var authorization = store.findAuthorizationById(authorizationId)
                .orElseThrow(SubjectOwnershipReassignmentAuthorizationException::new);
        var selectedIdentityId = store.lockCurrentPointer(command.reference());
        var afterLock = store.findCompletedReassignmentByRequestId(command.reassignmentRequestId());
        if (afterLock.isPresent()) return replayWithAuthorization(afterLock.get(), command, executor.principalId());

        var predecessor = selectedIdentityId
                .map(id -> store.lockSelectedIdentity(command.reference(), id)
                        .orElseThrow(() -> new IllegalStateException("Current pointer references a missing identity")))
                .orElseThrow(SubjectOwnershipIdentityNotFoundException::new);

        if (!predecessor.id().equals(command.predecessorIdentityId()))
            throw new SubjectOwnershipReassignmentConflictException("Predecessor is not current");
        if ("logos-native".equals(command.reference().namespace()))
            throw new InvalidSubjectOwnershipTransitionException("Native identities cannot be reassigned");
        if (!IdentityClass.EXTERNAL.name().equals(predecessor.identityClass())
                || !OwnershipStatus.REVOKED.name().equals(predecessor.ownershipStatus())
                || !supported(predecessor.verificationStatus()))
            throw new InvalidSubjectOwnershipTransitionException("Current predecessor is not eligible for reassignment");
        if (predecessor.ownershipVersion() != command.expectedPredecessorOwnershipVersion())
            throw new SubjectOwnershipReassignmentConflictException("Predecessor version conflict");
        validateAuthorization(authorization, command, predecessor);
        if (authorization.reviewerPrincipalId().equals(executor.principalId()))
            throw new SubjectOwnershipReassignmentAuthorizationException();
        if (predecessor.targetJogadorId().equals(command.newTargetJogadorId()))
            throw new InvalidSubjectOwnershipTransitionException("Successor target must differ from predecessor target");
        if (!store.targetExists(command.newTargetJogadorId()))
            throw new InvalidSubjectOwnershipTransitionException("New target jogador does not exist");

        var nextVerification = predecessor.verificationStatus() == VerificationStatus.VERIFIED
                ? VerificationStatus.UNVERIFIED : predecessor.verificationStatus();
        var successorId = UUID.randomUUID();
        store.createSuccessorAndHistory(predecessor, successorId, command.newTargetJogadorId(), nextVerification,
                command.reassignmentRequestId(), authorizationId, normalizedReason, executor, Instant.now());
        int changed = store.switchCurrentPointer(command.reference(), predecessor.id(), successorId);
        if (changed != 1) throw new SubjectOwnershipReassignmentConflictException("Current binding changed during reassignment");
        return result(successorId, command.reference(), command.newTargetJogadorId(), nextVerification,
                command.reassignmentRequestId(), authorizationId);
    }

    private SubjectOwnershipReassignmentResult replayWithAuthorization(
            SubjectOwnershipReassignmentStore.ReassignmentEvent event,
            ReassignExternalSubjectOwnershipCommand command, String executor) {
        var result = replay(event, command, executor);
        var authorization = store.findAuthorizationById(command.authorizationId())
                .orElseThrow(SubjectOwnershipReassignmentAuthorizationException::new);
        if (!authorization.reassignmentRequestId().equals(event.reassignmentRequestId())
                || !authorization.reference().equals(event.reference())
                || !authorization.predecessorIdentityId().equals(event.predecessorIdentityId())
                || authorization.predecessorOwnershipVersion() != event.predecessorOwnershipVersion()
                || !authorization.predecessorTargetJogadorId().equals(event.previousTargetJogadorId())
                || !authorization.proposedSuccessorTargetJogadorId().equals(event.newTargetJogadorId())
                || !authorization.authorizationId().equals(event.reassignmentAuthorizationId())
                || authorization.reviewerPrincipalId().equals(event.actorId()))
            throw new SubjectOwnershipReassignmentAuthorizationException();
        return result;
    }

    private static void validateAuthorization(SubjectOwnershipReassignmentAuthorization authorization,
            ReassignExternalSubjectOwnershipCommand command,
            SubjectOwnershipReassignmentStore.ReassignmentPredecessor predecessor) {
        if (!authorization.authorizationId().equals(command.authorizationId())
                || !authorization.reassignmentRequestId().equals(command.reassignmentRequestId())
                || !authorization.reference().equals(command.reference())
                || !authorization.predecessorIdentityId().equals(predecessor.id())
                || authorization.predecessorOwnershipVersion() != command.expectedPredecessorOwnershipVersion()
                || !authorization.predecessorTargetJogadorId().equals(predecessor.targetJogadorId())
                || !authorization.proposedSuccessorTargetJogadorId().equals(command.newTargetJogadorId()))
            throw new SubjectOwnershipReassignmentAuthorizationException();
    }

    private static SubjectOwnershipReassignmentResult replay(SubjectOwnershipReassignmentStore.ReassignmentEvent event,
            ReassignExternalSubjectOwnershipCommand command, String executor) {
        if (!event.reassignmentRequestId().equals(command.reassignmentRequestId())
                || !event.reassignmentAuthorizationId().equals(command.authorizationId())
                || !event.reference().equals(command.reference())
                || !event.predecessorIdentityId().equals(command.predecessorIdentityId())
                || event.predecessorOwnershipVersion() != command.expectedPredecessorOwnershipVersion()
                || !event.newTargetJogadorId().equals(command.newTargetJogadorId())
                || !event.reason().equals(command.reason().trim()) || !EVIDENCE_TYPE.equals(event.evidenceType())
                || !command.authorizationId().toString().equals(event.evidenceReference())
                || !"LOGOS_OPERATOR_ACTION".equals(event.provenance())
                || !"WORKLOAD_OPERATOR".equals(event.actorType()) || !executor.equals(event.actorId())
                || !"EXTERNAL".equals(event.previousIdentityClass())
                || !"EXTERNAL".equals(event.newIdentityClass())
                || !"REVOKED".equals(event.previousOwnershipStatus())
                || !"DISABLED".equals(event.newOwnershipStatus()) || event.aggregateVersion() != 0
                || event.previousTargetJogadorId().equals(event.newTargetJogadorId())
                || !supported(event.previousVerificationStatus())
                || event.newVerificationStatus() != mapped(event.previousVerificationStatus()))
            throw new SubjectOwnershipReassignmentConflictException("Completed reassignment request does not match command");
        OwnershipStatus creationOwnership;
        try {
            creationOwnership = OwnershipStatus.valueOf(event.newOwnershipStatus());
        } catch (RuntimeException invalidSnapshot) {
            throw new SubjectOwnershipReassignmentConflictException("Completed reassignment snapshot is invalid");
        }
        return new SubjectOwnershipReassignmentResult(event.identityId(), event.reference(), event.newTargetJogadorId(),
                creationOwnership, event.newVerificationStatus(), event.aggregateVersion(),
                event.reassignmentRequestId(), event.reassignmentAuthorizationId());
    }

    private static boolean supported(VerificationStatus status) {
        return status == VerificationStatus.UNVERIFIED || status == VerificationStatus.VERIFIED
                || status == VerificationStatus.INVALIDATED;
    }
    private static VerificationStatus mapped(VerificationStatus status) {
        return status == VerificationStatus.VERIFIED ? VerificationStatus.UNVERIFIED : status;
    }
    private static SubjectOwnershipReassignmentResult result(UUID id,
            com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference reference,
            UUID target, VerificationStatus verification, UUID request, UUID authorization) {
        return new SubjectOwnershipReassignmentResult(id, reference, target, OwnershipStatus.DISABLED,
                verification, 0L, request, authorization);
    }
}
