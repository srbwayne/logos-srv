package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import java.time.Instant;
import java.util.UUID;

public record SubjectOwnershipReassignmentAuthorization(UUID authorizationId, UUID reassignmentRequestId,
        ExternalSubjectReference reference, UUID predecessorIdentityId, long predecessorOwnershipVersion,
        UUID predecessorTargetJogadorId, UUID proposedSuccessorTargetJogadorId, String recoveryBasis,
        String reviewedCaseReference, String reviewerPrincipalId, Instant reviewedAt) {
    public String evidenceReference() { return authorizationId.toString(); }
}
