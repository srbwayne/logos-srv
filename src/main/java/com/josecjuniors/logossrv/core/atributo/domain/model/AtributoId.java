package com.josecjuniors.logossrv.core.atributo.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class AtributoId extends DomainObjectId {

    public AtributoId() {
        super();
    }

    public AtributoId(UUID value) {
        super(value);
    }


    public static AtributoId from(String id) {
        return new AtributoId(UUID.fromString(id));

    }

    public static AtributoId generate() {
        return new AtributoId(UUID.randomUUID());
    }
}
