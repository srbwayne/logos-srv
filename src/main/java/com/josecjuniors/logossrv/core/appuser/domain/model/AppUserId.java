package com.josecjuniors.logossrv.core.appuser.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class AppUserId extends DomainObjectId {
    public AppUserId() {
        super();
    }

    public AppUserId(UUID value) {
        super(value);
    }
}
