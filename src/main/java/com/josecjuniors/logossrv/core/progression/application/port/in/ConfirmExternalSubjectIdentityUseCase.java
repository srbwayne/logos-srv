package com.josecjuniors.logossrv.core.progression.application.port.in;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;

public interface ConfirmExternalSubjectIdentityUseCase {
    ExternalSubjectReference confirm(String namespace, String externalId, String challengeToken, String clientId);
}
