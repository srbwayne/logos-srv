package com.josecjuniors.logossrv.core.atividadeformulario.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class AtividadeFormularioId extends DomainObjectId {
    public AtividadeFormularioId() { super(); }
    public AtividadeFormularioId(UUID value) { super(value); }
    public static AtividadeFormularioId generate() { return new AtividadeFormularioId(UUID.randomUUID()); }
}
