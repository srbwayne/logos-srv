package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipStatus;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus;
import java.util.UUID;

public record SubjectOwnershipTargetCorrectionResult(UUID successorIdentityId,
        ExternalSubjectReference reference, UUID correctedTargetJogadorId, OwnershipStatus ownershipStatus,
        VerificationStatus verificationStatus, long ownershipVersion, UUID predecessorIdentityId,
        long predecessorOwnershipVersion, UUID correctionRequestId, UUID authorizationId) { }
