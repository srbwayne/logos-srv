package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;

public record ReactivateExternalSubjectOwnershipCommand(ExternalSubjectReference reference,
        long expectedOwnershipVersion, String reason) {
    public ReactivateExternalSubjectOwnershipCommand {
        if (reference == null) throw new IllegalArgumentException("reference is required");
        if (expectedOwnershipVersion < 0)
            throw new IllegalArgumentException("expectedOwnershipVersion must be nonnegative");
    }
}
