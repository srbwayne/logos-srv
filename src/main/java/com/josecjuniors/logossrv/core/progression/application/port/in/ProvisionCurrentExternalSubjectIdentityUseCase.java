package com.josecjuniors.logossrv.core.progression.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;

public interface ProvisionCurrentExternalSubjectIdentityUseCase {
    ExternalSubjectReference provision(String authenticatedUserEmail, ExternalSubjectReference reference);
}
