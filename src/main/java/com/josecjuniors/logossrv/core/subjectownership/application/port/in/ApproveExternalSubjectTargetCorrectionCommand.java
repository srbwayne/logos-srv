package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import java.util.UUID;

/** A reviewed correction request. Reviewer and authorization ID are server supplied. */
public record ApproveExternalSubjectTargetCorrectionCommand(ExternalSubjectReference reference,
        UUID predecessorIdentityId, long expectedPredecessorOwnershipVersion,
        UUID correctedTargetJogadorId, UUID correctionRequestId, String correctionBasis,
        String sourceAssertionReference, String authoritativeFactReference, String reviewedCaseReference) { }
