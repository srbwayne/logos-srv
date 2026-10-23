package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import java.time.Instant;
import java.util.UUID;

public record SubjectOwnershipTargetCorrectionAuthorization(UUID authorizationId, UUID correctionRequestId,
        ExternalSubjectReference reference, UUID predecessorIdentityId, long predecessorOwnershipVersion,
        UUID predecessorTargetJogadorId, UUID correctedTargetJogadorId, String correctionBasis,
        String sourceAssertionReference, String authoritativeFactReference, String reviewedCaseReference,
        String reviewerPrincipalId, Instant reviewedAt) { }
