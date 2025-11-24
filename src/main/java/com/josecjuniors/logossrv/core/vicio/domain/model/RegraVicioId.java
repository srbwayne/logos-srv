package com.josecjuniors.logossrv.core.vicio.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class RegraVicioId extends DomainObjectId {

    public RegraVicioId() {
        super();
    }

    public RegraVicioId(UUID value) {
        super(value);
    }

    public static RegraVicioId generate() {
        return new RegraVicioId(UUID.randomUUID());
    }
}
