package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import java.util.UUID;

public record ApproveExternalSubjectReassignmentCommand(ExternalSubjectReference reference,
        UUID predecessorIdentityId, long expectedPredecessorOwnershipVersion,
        UUID proposedSuccessorTargetJogadorId, UUID reassignmentRequestId,
        String recoveryBasis, String reviewedCaseReference) {
    public ApproveExternalSubjectReassignmentCommand {
        if (reference == null || predecessorIdentityId == null || proposedSuccessorTargetJogadorId == null
                || reassignmentRequestId == null) throw new IllegalArgumentException("required approval fields are missing");
        if (expectedPredecessorOwnershipVersion < 0) throw new IllegalArgumentException("version must be nonnegative");
        recoveryBasis = text(recoveryBasis, 2048, "recoveryBasis");
        reviewedCaseReference = text(reviewedCaseReference, 255, "reviewedCaseReference");
    }
    private static String text(String value, int max, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        value = value.trim();
        if (value.isEmpty() || value.length() > max) throw new IllegalArgumentException(name + " is invalid");
        return value;
    }
}
