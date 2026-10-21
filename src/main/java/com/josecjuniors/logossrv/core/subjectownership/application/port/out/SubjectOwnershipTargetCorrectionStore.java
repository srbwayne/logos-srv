package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SubjectOwnershipTargetCorrectionStore {
    Optional<UUID> lockCurrentPointer(ExternalSubjectReference reference);
    Optional<CorrectionPredecessor> lockSelectedIdentity(ExternalSubjectReference reference, UUID identityId);
    Optional<CorrectionEvent> findCompletedCorrectionByRequestId(UUID requestId);
    boolean hasTransferEvent(UUID identityId);
    boolean hasSuccessor(UUID identityId);
    boolean targetExists(UUID targetId);
    void insertSuccessor(CorrectionPredecessor predecessor, UUID successorId, UUID targetId,
            VerificationStatus verification);
    void insertCorrectionHistory(CorrectionPredecessor predecessor, UUID successorId, UUID targetId,
            VerificationStatus nextVerification, UUID requestId, UUID authorizationId, String reason,
            AuthorizedSubjectOwnershipOperator executor, Instant effectiveAt);
    int compareAndSetCurrentPointerForTargetCorrection(ExternalSubjectReference reference,
            UUID predecessorId, UUID successorId);

    record CorrectionPredecessor(UUID id, ExternalSubjectReference reference, UUID targetJogadorId,
            String identityClass, String ownershipStatus, VerificationStatus verificationStatus,
            long ownershipVersion, UUID predecessorIdentityId) { }

    /** All mutable identity values are intentionally excluded from the replay projection. */
    record CorrectionEvent(UUID identityId, long aggregateVersion, ExternalSubjectReference reference,
            UUID predecessorIdentityId, long predecessorOwnershipVersion, UUID previousTargetJogadorId,
            UUID newTargetJogadorId, String previousIdentityClass, String newIdentityClass,
            String previousOwnershipStatus, String newOwnershipStatus,
            VerificationStatus previousVerificationStatus, VerificationStatus newVerificationStatus,
            String provenance, String actorType, String actorId, String evidenceType, String evidenceReference,
            String reason, UUID correctionRequestId, UUID authorizationId) { }
}
