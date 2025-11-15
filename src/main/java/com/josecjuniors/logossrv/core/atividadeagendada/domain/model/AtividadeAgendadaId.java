package com.josecjuniors.logossrv.core.atividadeagendada.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class AtividadeAgendadaId extends DomainObjectId {
    public AtividadeAgendadaId() { super(); }
    public AtividadeAgendadaId(UUID value) { super(value); }
    public static AtividadeAgendadaId generate() { return new AtividadeAgendadaId(UUID.randomUUID()); }
}
