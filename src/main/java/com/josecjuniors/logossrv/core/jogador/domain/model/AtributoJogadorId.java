package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class AtributoJogadorId extends DomainObjectId {
    public AtributoJogadorId() {
        super();
    }

    public AtributoJogadorId(UUID value) {
        super(value);
    }

    public static AtributoJogadorId generate() {
        return new AtributoJogadorId(UUID.randomUUID());
    }
}
