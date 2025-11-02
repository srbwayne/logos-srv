package com.josecjuniors.logossrv.core.estresse.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class EstresseId extends DomainObjectId {

    public EstresseId() {
        super();
    }

    public EstresseId(UUID value) {
        super(value);
    }
}
