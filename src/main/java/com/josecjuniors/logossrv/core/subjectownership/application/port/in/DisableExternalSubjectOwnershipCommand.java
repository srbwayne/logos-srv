package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;

public record DisableExternalSubjectOwnershipCommand(ExternalSubjectReference reference,
        long expectedOwnershipVersion, String reason) {
    public DisableExternalSubjectOwnershipCommand {
        if (reference == null) throw new IllegalArgumentException("reference is required");
        if (expectedOwnershipVersion < 0)
            throw new IllegalArgumentException("expectedOwnershipVersion must be nonnegative");
    }
}
