package com.josecjuniors.logossrv.core.viciojogador.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class VicioJogadorId extends DomainObjectId {
    public VicioJogadorId() {
        super();
    }

    public VicioJogadorId(UUID value) {
        super(value);
    }
}
