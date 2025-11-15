package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class JogadorId extends DomainObjectId {

    protected JogadorId() {
        super();
    }

    public JogadorId(UUID value) {
        super(value);
    }

    /**
     * Gera um novo identificador único para Jogador.
     * @return um novo JogadorId.
     */
    public static JogadorId generate() {
        return new JogadorId(UUID.randomUUID());
    }
}
