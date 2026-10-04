package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;

public record ReverifyExternalSubjectOwnershipCommand(ExternalSubjectReference reference,
        long expectedOwnershipVersion, String evidenceType, String evidenceReference, String reason) {
    public ReverifyExternalSubjectOwnershipCommand {
        if (reference == null) throw new IllegalArgumentException("reference is required");
        if (expectedOwnershipVersion < 0) throw new IllegalArgumentException("expectedOwnershipVersion must be nonnegative");
    }
}
