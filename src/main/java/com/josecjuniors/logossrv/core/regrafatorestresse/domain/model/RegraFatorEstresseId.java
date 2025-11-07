package com.josecjuniors.logossrv.core.regrafatorestresse.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class RegraFatorEstresseId extends DomainObjectId {
    public RegraFatorEstresseId() {
        super();
    }

    public RegraFatorEstresseId(UUID value) {
        super(value);
    }
}
