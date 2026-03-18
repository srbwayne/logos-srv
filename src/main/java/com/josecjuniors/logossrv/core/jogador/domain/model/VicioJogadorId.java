package com.josecjuniors.logossrv.core.jogador.domain.model;

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

    public static VicioJogadorId generate() {
        return new VicioJogadorId(UUID.randomUUID());
    }
}
