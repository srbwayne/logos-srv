package com.josecjuniors.logossrv.core.registroatividade.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class RegistroAtividadeId extends DomainObjectId {

    public RegistroAtividadeId() {
        super();
    }

    public RegistroAtividadeId(UUID value) {
        super(value);
    }
}
