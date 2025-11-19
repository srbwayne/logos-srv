package com.josecjuniors.logossrv.core.registroatividade.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class RegistroAtividadeDetalheId extends DomainObjectId {
    public RegistroAtividadeDetalheId() { super(); }
    public RegistroAtividadeDetalheId(UUID value) { super(value); }
    public static RegistroAtividadeDetalheId generate() { return new RegistroAtividadeDetalheId(UUID.randomUUID()); }
}
