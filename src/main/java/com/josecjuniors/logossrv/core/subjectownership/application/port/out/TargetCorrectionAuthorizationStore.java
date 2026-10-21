package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TargetCorrectionAuthorizationStore {
    Optional<SubjectOwnershipTargetCorrectionAuthorization> findAuthorizationById(UUID id);
    Optional<SubjectOwnershipTargetCorrectionAuthorization> findAuthorizationByRequestId(UUID requestId);
    SubjectOwnershipTargetCorrectionAuthorization insertAuthorizationIfAbsent(
            SubjectOwnershipTargetCorrectionAuthorization proposed);
}
