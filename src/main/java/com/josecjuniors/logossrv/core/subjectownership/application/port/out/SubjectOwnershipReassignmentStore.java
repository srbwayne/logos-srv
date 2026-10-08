package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SubjectOwnershipReassignmentStore {
    Optional<SubjectOwnershipReassignmentAuthorization> findAuthorizationById(UUID id);
    Optional<SubjectOwnershipReassignmentAuthorization> findAuthorizationByRequestId(UUID requestId);
    SubjectOwnershipReassignmentAuthorization insertAuthorizationIfAbsent(SubjectOwnershipReassignmentAuthorization proposed);
    Optional<UUID> lockCurrentPointer(ExternalSubjectReference reference);
    Optional<ReassignmentPredecessor> lockSelectedIdentity(ExternalSubjectReference reference, UUID identityId);
    Optional<ReassignmentPredecessor> lockCurrentPredecessor(ExternalSubjectReference reference);
    Optional<ReassignmentEvent> findCompletedReassignmentByRequestId(UUID requestId);
    boolean targetExists(UUID targetId);
    void createSuccessorAndHistory(ReassignmentPredecessor predecessor, UUID successorId, UUID targetId,
            VerificationStatus verification, UUID requestId, UUID authorizationId, String reason,
            AuthorizedSubjectOwnershipOperator executor, Instant effectiveAt);
    int switchCurrentPointer(ExternalSubjectReference reference, UUID predecessorId, UUID successorId);

    record ReassignmentPredecessor(UUID id, ExternalSubjectReference reference, UUID targetJogadorId,
            String identityClass, String ownershipStatus, VerificationStatus verificationStatus, long ownershipVersion) { }

    record ReassignmentEvent(UUID identityId, long aggregateVersion, ExternalSubjectReference reference,
            String previousIdentityClass, String newIdentityClass, UUID newTargetJogadorId,
            String newOwnershipStatus, VerificationStatus newVerificationStatus,
            UUID previousTargetJogadorId, String previousOwnershipStatus,
            VerificationStatus previousVerificationStatus, long predecessorOwnershipVersion,
            String reason, String evidenceType, String evidenceReference, String provenance,
            String actorType, String actorId, UUID reassignmentRequestId, UUID reassignmentAuthorizationId,
            UUID predecessorIdentityId) { }
}
