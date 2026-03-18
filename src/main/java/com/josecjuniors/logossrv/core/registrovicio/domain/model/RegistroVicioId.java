package com.josecjuniors.logossrv.core.registrovicio.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class RegistroVicioId extends DomainObjectId {
    public RegistroVicioId() {
        super();
    }

    public RegistroVicioId(UUID value) {
        super(value);
    }

    public static RegistroVicioId generate() {
        return new RegistroVicioId(UUID.randomUUID());
    }
}
