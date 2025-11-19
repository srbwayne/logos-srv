package com.josecjuniors.logossrv.core.atividadeconfig.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class AtividadeConfigId extends DomainObjectId {

    public AtividadeConfigId() {
        super();
    }

    public AtividadeConfigId(UUID value) {
        super(value);
    }

    public static AtividadeConfigId generate() {
        return new AtividadeConfigId(UUID.randomUUID());
    }
}
