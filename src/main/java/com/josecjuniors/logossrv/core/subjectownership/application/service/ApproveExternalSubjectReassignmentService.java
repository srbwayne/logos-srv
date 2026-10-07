package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ApproveExternalSubjectReassignmentCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ApproveExternalSubjectReassignmentUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentAuthorization;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipReassignmentConflictException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveExternalSubjectReassignmentService implements ApproveExternalSubjectReassignmentUseCase {
    private final SubjectOwnershipOperatorContext operators;
    private final SubjectOwnershipReassignmentStore store;

    public ApproveExternalSubjectReassignmentService(SubjectOwnershipOperatorContext operators,
            SubjectOwnershipReassignmentStore store) {
        this.operators = operators;
        this.store = store;
    }

    @Override
    @Transactional
    public SubjectOwnershipReassignmentAuthorization approve(ApproveExternalSubjectReassignmentCommand command) {
        var operator = operators.authorizeForNamespace(command.reference().namespace());
        if (!operator.namespace().equals(command.reference().namespace()))
            throw new SecurityException("Namespace authorization mismatch");

        var existing = store.findAuthorizationByRequestId(command.reassignmentRequestId());
        if (existing.isPresent()) return resolveRetry(existing.get(), command, operator.principalId());

        var predecessor = store.lockCurrentPredecessor(command.reference())
                .orElseThrow(() -> new InvalidSubjectOwnershipTransitionException("Current predecessor was not found"));
        validatePredecessor(command, predecessor);
        if (!store.targetExists(command.proposedSuccessorTargetJogadorId()))
            throw new InvalidSubjectOwnershipTransitionException("Proposed successor target does not exist");
        if (predecessor.targetJogadorId().equals(command.proposedSuccessorTargetJogadorId()))
            throw new InvalidSubjectOwnershipTransitionException("Successor target must differ from predecessor target");

        var proposed = new SubjectOwnershipReassignmentAuthorization(UUID.randomUUID(), command.reassignmentRequestId(),
                command.reference(), predecessor.id(), predecessor.ownershipVersion(), predecessor.targetJogadorId(),
                command.proposedSuccessorTargetJogadorId(), command.recoveryBasis(), command.reviewedCaseReference(),
                operator.principalId(), null);
        var stored = store.insertAuthorizationIfAbsent(proposed);
        return resolveRetry(stored, command, operator.principalId());
    }

    private static void validatePredecessor(ApproveExternalSubjectReassignmentCommand command,
            SubjectOwnershipReassignmentStore.ReassignmentPredecessor predecessor) {
        if ("logos-native".equals(command.reference().namespace()))
            throw new InvalidSubjectOwnershipTransitionException("Native identities cannot be reassigned");
        if (!predecessor.id().equals(command.predecessorIdentityId()))
            throw new SubjectOwnershipReassignmentConflictException("Predecessor is not current");
        if (!"EXTERNAL".equals(predecessor.identityClass()) || !"REVOKED".equals(predecessor.ownershipStatus())
                || predecessor.verificationStatus() == null
                || !(predecessor.verificationStatus().name().equals("UNVERIFIED")
                    || predecessor.verificationStatus().name().equals("VERIFIED")
                    || predecessor.verificationStatus().name().equals("INVALIDATED")))
            throw new InvalidSubjectOwnershipTransitionException("Predecessor is not eligible for reassignment approval");
        if (predecessor.ownershipVersion() != command.expectedPredecessorOwnershipVersion())
            throw new SubjectOwnershipReassignmentConflictException("Predecessor version conflict");
    }

    private static SubjectOwnershipReassignmentAuthorization resolveRetry(
            SubjectOwnershipReassignmentAuthorization existing, ApproveExternalSubjectReassignmentCommand command,
            String reviewer) {
        if (!existing.reference().equals(command.reference())
                || !existing.predecessorIdentityId().equals(command.predecessorIdentityId())
                || existing.predecessorOwnershipVersion() != command.expectedPredecessorOwnershipVersion()
                || !existing.proposedSuccessorTargetJogadorId().equals(command.proposedSuccessorTargetJogadorId())
                || !existing.recoveryBasis().equals(command.recoveryBasis())
                || !existing.reviewedCaseReference().equals(command.reviewedCaseReference())
                || !existing.reviewerPrincipalId().equals(reviewer))
            throw new SubjectOwnershipReassignmentConflictException("Reassignment request is already bound to different reviewed facts");
        return existing;
    }
}
