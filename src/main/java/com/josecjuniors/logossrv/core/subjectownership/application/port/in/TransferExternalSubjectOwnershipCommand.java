package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import java.util.UUID;

public record TransferExternalSubjectOwnershipCommand(ExternalSubjectReference reference,
        long expectedOwnershipVersion, UUID newTargetJogadorId, String transferEvidenceReference, String reason) {
    public TransferExternalSubjectOwnershipCommand {
        if (reference == null) throw new IllegalArgumentException("reference is required");
        if (expectedOwnershipVersion < 0)
            throw new IllegalArgumentException("expectedOwnershipVersion must be nonnegative");
        if (newTargetJogadorId == null) throw new IllegalArgumentException("newTargetJogadorId is required");
    }
}
