package com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class RegraDistribuicaoAtividadeId extends DomainObjectId {
    public RegraDistribuicaoAtividadeId() {
        super();
    }

    public RegraDistribuicaoAtividadeId(UUID value) {
        super(value);
    }
}
