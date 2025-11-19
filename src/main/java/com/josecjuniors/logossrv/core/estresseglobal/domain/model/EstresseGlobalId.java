package com.josecjuniors.logossrv.core.estresseglobal.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class EstresseGlobalId extends DomainObjectId {

    public EstresseGlobalId() {
        super();
    }

    public EstresseGlobalId(UUID value) {
        super(value);
    }

    public static EstresseGlobalId generate() {
        return new EstresseGlobalId(UUID.randomUUID());
    }
}
