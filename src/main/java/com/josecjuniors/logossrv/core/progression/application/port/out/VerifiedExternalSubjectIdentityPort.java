package com.josecjuniors.logossrv.core.progression.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface VerifiedExternalSubjectIdentityPort {
    void confirm(String namespace, String externalId, UUID jogadorId, String clientId, Instant verifiedAt);
}
