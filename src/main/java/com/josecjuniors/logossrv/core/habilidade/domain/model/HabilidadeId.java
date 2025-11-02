package com.josecjuniors.logossrv.core.habilidade.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class HabilidadeId extends DomainObjectId {

    public HabilidadeId() {
        super();
    }

    public HabilidadeId(UUID value) {
        super(value);
    }
}
