package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.CorrectExternalSubjectTargetCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.CorrectExternalSubjectTargetUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionAuthorization;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionResult;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.TargetCorrectionAuthorizationStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipTargetCorrectionAuthorizationException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipTargetCorrectionConflictException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.IdentityClass;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipStatus;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrectExternalSubjectTargetService implements CorrectExternalSubjectTargetUseCase {
    private static final String EVIDENCE_TYPE = "ADMINISTRATIVE_TARGET_CORRECTION_AUTHORIZATION";
    private final SubjectOwnershipOperatorContext operators;
    private final TargetCorrectionAuthorizationStore authorizations;
    private final SubjectOwnershipTargetCorrectionStore corrections;

    public CorrectExternalSubjectTargetService(SubjectOwnershipOperatorContext operators,
            TargetCorrectionAuthorizationStore authorizations, SubjectOwnershipTargetCorrectionStore corrections) {
        this.operators = operators;
        this.authorizations = authorizations;
        this.corrections = corrections;
    }

    @Override @Transactional
    public SubjectOwnershipTargetCorrectionResult correct(CorrectExternalSubjectTargetCommand command) {
        var executor = operators.authorizeForNamespace(command.reference().namespace());
        if (!executor.namespace().equals(command.reference().namespace()))
            throw new SecurityException("Namespace authorization mismatch");
        validateSyntax(command);

        var initial = corrections.findCompletedCorrectionByRequestId(command.correctionRequestId());
        if (initial.isPresent()) return replayWithAuthorization(initial.get(), command, executor.principalId());

        var authorization = authorizations.findAuthorizationById(command.targetCorrectionAuthorizationId())
                .orElseThrow(SubjectOwnershipTargetCorrectionAuthorizationException::new);
        var selectedIdentityId = corrections.lockCurrentPointer(command.reference());
        var afterLock = corrections.findCompletedCorrectionByRequestId(command.correctionRequestId());
        if (afterLock.isPresent()) return replayWithAuthorization(afterLock.get(), command, executor.principalId());

        var predecessor = selectedIdentityId
                .map(id -> corrections.lockSelectedIdentity(command.reference(), id)
                        .orElseThrow(() -> new DataIntegrityViolationException(
                                "Current pointer references a missing identity")))
                .orElseThrow(SubjectOwnershipIdentityNotFoundException::new);
        validatePredecessor(command, predecessor);
        if (corrections.hasTransferEvent(predecessor.id()))
            throw new InvalidSubjectOwnershipTransitionException("Transferred predecessor cannot be corrected");
        if (corrections.hasSuccessor(predecessor.id()))
            throw new InvalidSubjectOwnershipTransitionException("Predecessor already has a successor");
        validateAuthorization(authorization, command, predecessor);
        if (authorization.reviewerPrincipalId().equals(executor.principalId()))
            throw new SubjectOwnershipTargetCorrectionAuthorizationException();
        if (predecessor.targetJogadorId().equals(command.correctedTargetJogadorId()))
            throw new InvalidSubjectOwnershipTransitionException("Corrected target must differ from predecessor target");
        if (!corrections.targetExists(command.correctedTargetJogadorId()))
            throw new InvalidSubjectOwnershipTransitionException("Corrected target does not exist");

        VerificationStatus nextVerification = mapped(predecessor.verificationStatus());
        UUID successorId = UUID.randomUUID();
        corrections.insertSuccessor(predecessor, successorId, command.correctedTargetJogadorId(), nextVerification);
        corrections.insertCorrectionHistory(predecessor, successorId, command.correctedTargetJogadorId(),
                nextVerification, command.correctionRequestId(), command.targetCorrectionAuthorizationId(),
                command.reason().trim(), executor, Instant.now());
        int changed = corrections.compareAndSetCurrentPointerForTargetCorrection(command.reference(),
                predecessor.id(), successorId);
        if (changed == 0)
            throw new SubjectOwnershipTargetCorrectionConflictException("Current binding changed during target correction");
        if (changed != 1)
            throw new DataIntegrityViolationException("Target correction pointer CAS affected more than one row");
        return result(successorId, command.reference(), command.correctedTargetJogadorId(), nextVerification,
                predecessor.id(), predecessor.ownershipVersion(), command.correctionRequestId(),
                command.targetCorrectionAuthorizationId());
    }

    private static void validateSyntax(CorrectExternalSubjectTargetCommand command) {
        if (command.predecessorIdentityId() == null || command.correctedTargetJogadorId() == null
                || command.correctionRequestId() == null || command.targetCorrectionAuthorizationId() == null
                || command.expectedPredecessorOwnershipVersion() < 0)
            throw new IllegalArgumentException("Required target correction fields are invalid");
        if (command.reason() == null) throw new IllegalArgumentException("Reason is required");
        String reason = command.reason().trim();
        if (reason.isEmpty() || reason.length() > 512)
            throw new IllegalArgumentException("Reason must be nonblank and at most 512 characters");
    }

    private static void validatePredecessor(CorrectExternalSubjectTargetCommand command,
            SubjectOwnershipTargetCorrectionStore.CorrectionPredecessor predecessor) {
        if ("logos-native".equals(command.reference().namespace()))
            throw new InvalidSubjectOwnershipTransitionException("Native identities cannot be corrected");
        if (!predecessor.id().equals(command.predecessorIdentityId()))
            throw new SubjectOwnershipTargetCorrectionConflictException("Predecessor is not current");
        if (predecessor.predecessorIdentityId() != null)
            throw new InvalidSubjectOwnershipTransitionException("Only root identities may be corrected");
        if (!IdentityClass.EXTERNAL.name().equals(predecessor.identityClass())
                || !(OwnershipStatus.ACTIVE.name().equals(predecessor.ownershipStatus())
                    || OwnershipStatus.DISABLED.name().equals(predecessor.ownershipStatus()))
                || !supported(predecessor.verificationStatus()))
            throw new InvalidSubjectOwnershipTransitionException("Current predecessor is not eligible for target correction");
        if (predecessor.ownershipVersion() != command.expectedPredecessorOwnershipVersion())
            throw new SubjectOwnershipTargetCorrectionConflictException("Predecessor version conflict");
    }

    private static void validateAuthorization(SubjectOwnershipTargetCorrectionAuthorization authorization,
            CorrectExternalSubjectTargetCommand command,
            SubjectOwnershipTargetCorrectionStore.CorrectionPredecessor predecessor) {
        if (!authorization.authorizationId().equals(command.targetCorrectionAuthorizationId())
                || !authorization.correctionRequestId().equals(command.correctionRequestId())
                || !authorization.reference().equals(command.reference())
                || !authorization.predecessorIdentityId().equals(predecessor.id())
                || authorization.predecessorOwnershipVersion() != command.expectedPredecessorOwnershipVersion()
                || !authorization.predecessorTargetJogadorId().equals(predecessor.targetJogadorId())
                || !authorization.correctedTargetJogadorId().equals(command.correctedTargetJogadorId()))
            throw new SubjectOwnershipTargetCorrectionAuthorizationException();
    }

    private SubjectOwnershipTargetCorrectionResult replayWithAuthorization(
            SubjectOwnershipTargetCorrectionStore.CorrectionEvent event,
            CorrectExternalSubjectTargetCommand command, String executor) {
        var result = replay(event, command, executor);
        var authorization = authorizations.findAuthorizationById(command.targetCorrectionAuthorizationId())
                .orElseThrow(SubjectOwnershipTargetCorrectionAuthorizationException::new);
        if (!authorization.correctionRequestId().equals(event.correctionRequestId())
                || !authorization.reference().equals(event.reference())
                || !authorization.predecessorIdentityId().equals(event.predecessorIdentityId())
                || authorization.predecessorOwnershipVersion() != event.predecessorOwnershipVersion()
                || !authorization.predecessorTargetJogadorId().equals(event.previousTargetJogadorId())
                || !authorization.correctedTargetJogadorId().equals(event.newTargetJogadorId())
                || !authorization.authorizationId().equals(event.authorizationId())
                || authorization.reviewerPrincipalId().equals(event.actorId()))
            throw new SubjectOwnershipTargetCorrectionAuthorizationException();
        return result;
    }

    private static SubjectOwnershipTargetCorrectionResult replay(
            SubjectOwnershipTargetCorrectionStore.CorrectionEvent event,
            CorrectExternalSubjectTargetCommand command, String executor) {
        if (!event.correctionRequestId().equals(command.correctionRequestId())
                || !event.reference().equals(command.reference())
                || !event.predecessorIdentityId().equals(command.predecessorIdentityId())
                || event.predecessorOwnershipVersion() != command.expectedPredecessorOwnershipVersion()
                || !event.newTargetJogadorId().equals(command.correctedTargetJogadorId())
                || !event.reason().equals(command.reason().trim())
                || !"WORKLOAD_OPERATOR".equals(event.actorType()) || !executor.equals(event.actorId()))
            throw new SubjectOwnershipTargetCorrectionConflictException(
                    "Completed target correction request does not match command");
        if (!event.authorizationId().equals(command.targetCorrectionAuthorizationId()))
            throw new SubjectOwnershipTargetCorrectionAuthorizationException();
        if (event.aggregateVersion() != 0 || !"EXTERNAL".equals(event.previousIdentityClass())
                || !"EXTERNAL".equals(event.newIdentityClass())
                || !("ACTIVE".equals(event.previousOwnershipStatus())
                    || "DISABLED".equals(event.previousOwnershipStatus()))
                || !"DISABLED".equals(event.newOwnershipStatus())
                || event.previousTargetJogadorId().equals(event.newTargetJogadorId())
                || !supported(event.previousVerificationStatus())
                || event.newVerificationStatus() != mapped(event.previousVerificationStatus())
                || !"LOGOS_OPERATOR_ACTION".equals(event.provenance())
                || !EVIDENCE_TYPE.equals(event.evidenceType())
                || !command.targetCorrectionAuthorizationId().toString().equals(event.evidenceReference()))
            throw new DataIntegrityViolationException("Stored target correction creation event is invalid");
        return result(event.identityId(), event.reference(), event.newTargetJogadorId(),
                event.newVerificationStatus(), event.predecessorIdentityId(),
                event.predecessorOwnershipVersion(), event.correctionRequestId(), event.authorizationId());
    }

    private static boolean supported(VerificationStatus status) {
        return status == VerificationStatus.UNVERIFIED || status == VerificationStatus.VERIFIED
                || status == VerificationStatus.INVALIDATED;
    }

    private static VerificationStatus mapped(VerificationStatus status) {
        if (!supported(status)) throw new InvalidSubjectOwnershipTransitionException(
                "Verification state is not eligible for target correction");
        return status == VerificationStatus.VERIFIED ? VerificationStatus.UNVERIFIED : status;
    }

    private static SubjectOwnershipTargetCorrectionResult result(UUID id,
            com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference reference,
            UUID target, VerificationStatus verification, UUID predecessor, long predecessorVersion,
            UUID request, UUID authorization) {
        return new SubjectOwnershipTargetCorrectionResult(id, reference, target, OwnershipStatus.DISABLED,
                verification, 0L, predecessor, predecessorVersion, request, authorization);
    }
}
