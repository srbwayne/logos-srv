package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import java.util.UUID;

public record CorrectExternalSubjectTargetCommand(ExternalSubjectReference reference,
        UUID predecessorIdentityId, long expectedPredecessorOwnershipVersion,
        UUID correctedTargetJogadorId, UUID correctionRequestId,
        UUID targetCorrectionAuthorizationId, String reason) { }
