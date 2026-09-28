package com.josecjuniors.logossrv.core.security.workload.admin.domain;

import java.util.Objects;

public record TrustAdministrationActor(TrustAdministrationActorType actorType, String actorId) {
    public TrustAdministrationActor {
        Objects.requireNonNull(actorType, "actorType");
        if (actorId == null || actorId.isBlank() || actorId.length() > 128) {
            throw new IllegalArgumentException("actorId must be nonblank and at most 128 characters");
        }
    }
}
