package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import java.util.UUID;

public record ReassignExternalSubjectOwnershipCommand(ExternalSubjectReference reference,
        UUID predecessorIdentityId, long expectedPredecessorOwnershipVersion,
        UUID newTargetJogadorId, UUID reassignmentRequestId, String evidenceReference, String reason) {
    public ReassignExternalSubjectOwnershipCommand {
        // Validation intentionally occurs in the use case after namespace authorization.
    }
    public UUID authorizationId() {
        if (evidenceReference == null || !UUID.fromString(evidenceReference).toString().equals(evidenceReference))
            throw new IllegalArgumentException("evidenceReference must be a canonical UUID");
        return UUID.fromString(evidenceReference);
    }
}
