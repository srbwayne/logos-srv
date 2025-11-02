package com.josecjuniors.logossrv.core.regravicio.domain.model;

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
}
