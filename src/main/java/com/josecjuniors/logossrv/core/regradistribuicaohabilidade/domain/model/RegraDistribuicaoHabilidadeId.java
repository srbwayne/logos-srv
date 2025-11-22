package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class RegraDistribuicaoHabilidadeId extends DomainObjectId {
    public RegraDistribuicaoHabilidadeId() {
        super();
    }

    public RegraDistribuicaoHabilidadeId(UUID value) {
        super(value);
    }

    public static RegraDistribuicaoHabilidadeId generate() {
        return new RegraDistribuicaoHabilidadeId(UUID.randomUUID());
    }
}
