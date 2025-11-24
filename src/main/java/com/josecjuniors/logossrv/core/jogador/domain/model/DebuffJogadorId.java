package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class DebuffJogadorId extends DomainObjectId {
    public DebuffJogadorId() { super(); }
    public DebuffJogadorId(UUID value) { super(value); }
    public static DebuffJogadorId generate() { return new DebuffJogadorId(UUID.randomUUID()); }
}
