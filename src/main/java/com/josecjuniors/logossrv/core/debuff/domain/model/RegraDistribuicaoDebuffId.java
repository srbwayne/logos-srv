package com.josecjuniors.logossrv.core.debuff.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class RegraDistribuicaoDebuffId extends DomainObjectId {
    public RegraDistribuicaoDebuffId() { super(); }
    public RegraDistribuicaoDebuffId(UUID value) { super(value); }
    public static RegraDistribuicaoDebuffId generate() { return new RegraDistribuicaoDebuffId(UUID.randomUUID()); }
}
