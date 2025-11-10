package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class HabilidadeJogadorId extends DomainObjectId {
    public HabilidadeJogadorId() {
        super();
    }

    public HabilidadeJogadorId(UUID value) {
        super(value);
    }
}
