package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class JogadorId extends DomainObjectId {

    public JogadorId() {
        super();
    }

    public JogadorId(UUID value) {
        super(value);
    }
}
