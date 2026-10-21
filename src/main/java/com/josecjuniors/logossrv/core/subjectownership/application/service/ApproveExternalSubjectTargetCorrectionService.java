package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ApproveExternalSubjectTargetCorrectionCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ApproveExternalSubjectTargetCorrectionUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionAuthorization;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.TargetCorrectionAuthorizationStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipTargetCorrectionConflictException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveExternalSubjectTargetCorrectionService
        implements ApproveExternalSubjectTargetCorrectionUseCase {
    private final SubjectOwnershipOperatorContext operators;
    private final TargetCorrectionAuthorizationStore authorizations;
    private final SubjectOwnershipTargetCorrectionStore corrections;

    public ApproveExternalSubjectTargetCorrectionService(SubjectOwnershipOperatorContext operators,
            TargetCorrectionAuthorizationStore authorizations, SubjectOwnershipTargetCorrectionStore corrections) {
        this.operators = operators;
        this.authorizations = authorizations;
        this.corrections = corrections;
    }

    @Override @Transactional
    public SubjectOwnershipTargetCorrectionAuthorization approve(ApproveExternalSubjectTargetCorrectionCommand command) {
        var operator = operators.authorizeForNamespace(command.reference().namespace());
        if (!operator.namespace().equals(command.reference().namespace()))
            throw new SecurityException("Namespace authorization mismatch");

        var normalized = validateSyntax(command);
        var existing = authorizations.findAuthorizationByRequestId(command.correctionRequestId());
        if (existing.isPresent()) return resolveRetry(existing.get(), command, operator.principalId());

        var selectedId = corrections.lockCurrentPointer(command.reference())
                .orElseThrow(() -> new InvalidSubjectOwnershipTransitionException("Current predecessor was not found"));
        var afterLock = authorizations.findAuthorizationByRequestId(command.correctionRequestId());
        if (afterLock.isPresent()) return resolveRetry(afterLock.get(), command, operator.principalId());

        var predecessor = corrections.lockSelectedIdentity(command.reference(), selectedId)
                .orElseThrow(() -> new IllegalStateException("Current pointer references a missing identity"));
        validatePredecessor(command, predecessor);
        if (corrections.hasTransferEvent(predecessor.id()))
            throw new InvalidSubjectOwnershipTransitionException("Transferred predecessor cannot be corrected");
        if (corrections.hasSuccessor(predecessor.id()))
            throw new InvalidSubjectOwnershipTransitionException("Predecessor already has a successor");
        if (!corrections.targetExists(command.correctedTargetJogadorId()))
            throw new InvalidSubjectOwnershipTransitionException("Corrected target does not exist");
        if (predecessor.targetJogadorId().equals(command.correctedTargetJogadorId()))
            throw new InvalidSubjectOwnershipTransitionException("Corrected target must differ from predecessor target");

        var proposed = new SubjectOwnershipTargetCorrectionAuthorization(UUID.randomUUID(),
                command.correctionRequestId(), command.reference(), predecessor.id(), predecessor.ownershipVersion(),
                predecessor.targetJogadorId(), command.correctedTargetJogadorId(), normalized.correctionBasis(),
                normalized.sourceAssertionReference(), normalized.authoritativeFactReference(),
                normalized.reviewedCaseReference(), operator.principalId(), null);
        return resolveRetry(authorizations.insertAuthorizationIfAbsent(proposed), command, operator.principalId());
    }

    private static NormalizedPayload validateSyntax(ApproveExternalSubjectTargetCorrectionCommand command) {
        if (command.predecessorIdentityId() == null || command.correctedTargetJogadorId() == null
                || command.correctionRequestId() == null || command.expectedPredecessorOwnershipVersion() < 0)
            throw new IllegalArgumentException("Required target correction approval fields are invalid");
        return new NormalizedPayload(text(command.correctionBasis(), 2048, "correctionBasis"),
                text(command.sourceAssertionReference(), 512, "sourceAssertionReference"),
                text(command.authoritativeFactReference(), 512, "authoritativeFactReference"),
                text(command.reviewedCaseReference(), 255, "reviewedCaseReference"));
    }

    private static String text(String value, int max, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > max)
            throw new IllegalArgumentException(name + " is invalid");
        return normalized;
    }

    private static void validatePredecessor(ApproveExternalSubjectTargetCorrectionCommand command,
            SubjectOwnershipTargetCorrectionStore.CorrectionPredecessor predecessor) {
        if ("logos-native".equals(command.reference().namespace()))
            throw new InvalidSubjectOwnershipTransitionException("Native identities cannot be corrected");
        if (!predecessor.id().equals(command.predecessorIdentityId()))
            throw new SubjectOwnershipTargetCorrectionConflictException("Predecessor is not current");
        if (predecessor.predecessorIdentityId() != null)
            throw new InvalidSubjectOwnershipTransitionException("Only root identities may be corrected");
        if (!"EXTERNAL".equals(predecessor.identityClass())
                || !("ACTIVE".equals(predecessor.ownershipStatus()) || "DISABLED".equals(predecessor.ownershipStatus()))
                || !supported(predecessor.verificationStatus()))
            throw new InvalidSubjectOwnershipTransitionException("Predecessor is not eligible for target correction");
        if (predecessor.ownershipVersion() != command.expectedPredecessorOwnershipVersion())
            throw new SubjectOwnershipTargetCorrectionConflictException("Predecessor version conflict");
    }

    private static boolean supported(com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus status) {
        return status == com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus.UNVERIFIED
                || status == com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus.VERIFIED
                || status == com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus.INVALIDATED;
    }

    private static SubjectOwnershipTargetCorrectionAuthorization resolveRetry(
            SubjectOwnershipTargetCorrectionAuthorization existing,
            ApproveExternalSubjectTargetCorrectionCommand command, String reviewer) {
        if (!existing.reference().equals(command.reference())
                || !existing.predecessorIdentityId().equals(command.predecessorIdentityId())
                || existing.predecessorOwnershipVersion() != command.expectedPredecessorOwnershipVersion()
                || !existing.correctedTargetJogadorId().equals(command.correctedTargetJogadorId())
                || !existing.correctionBasis().equals(command.correctionBasis().trim())
                || !existing.sourceAssertionReference().equals(command.sourceAssertionReference().trim())
                || !existing.authoritativeFactReference().equals(command.authoritativeFactReference().trim())
                || !existing.reviewedCaseReference().equals(command.reviewedCaseReference().trim())
                || !existing.reviewerPrincipalId().equals(reviewer))
            throw new SubjectOwnershipTargetCorrectionConflictException(
                    "Correction request is already bound to different reviewed facts");
        return existing;
    }

    private record NormalizedPayload(String correctionBasis, String sourceAssertionReference,
            String authoritativeFactReference, String reviewedCaseReference) { }
}
