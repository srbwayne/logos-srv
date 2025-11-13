package com.josecjuniors.logossrv.core.fatorcalculo.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class FatorCalculoId extends DomainObjectId {

    protected FatorCalculoId() {
        super();
    }

    public FatorCalculoId(UUID value) {
        super(value);
    }

    /**
     * Gera um novo identificador único para FatorCalculo.
     * @return um novo FatorCalculoId.
     */
    public static FatorCalculoId generate() {
        return new FatorCalculoId(UUID.randomUUID());
    }
}
