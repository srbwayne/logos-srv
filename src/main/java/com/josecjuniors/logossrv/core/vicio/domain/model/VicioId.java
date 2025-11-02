package com.josecjuniors.logossrv.core.vicio.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class VicioId extends DomainObjectId {
    public VicioId() {
        super();
    }

    public VicioId(UUID value) {
        super(value);
    }
}
