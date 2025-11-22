package com.josecjuniors.logossrv.core.habilidade.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class HabilidadeRequisitoId extends DomainObjectId {
    public HabilidadeRequisitoId() {
        super();
    }

    public HabilidadeRequisitoId(UUID value) {
        super(value);
    }

    public static HabilidadeRequisitoId generate() {
        return new HabilidadeRequisitoId(UUID.randomUUID());
    }
}
