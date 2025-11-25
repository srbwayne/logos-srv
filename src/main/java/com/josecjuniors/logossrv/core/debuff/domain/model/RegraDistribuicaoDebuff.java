package com.josecjuniors.logossrv.core.debuff.domain.model;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "regra_distribuicao_debuff")
public class RegraDistribuicaoDebuff extends AbstractDomainAggregate<RegraDistribuicaoDebuffId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debuff_id", nullable = false)
    private Debuff debuff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_id", nullable = false)
    private Atributo atributo;

    protected RegraDistribuicaoDebuff() {
        super();
    }

    public RegraDistribuicaoDebuff(RegraDistribuicaoDebuffId id, Debuff debuff, Atributo atributo) {
        super(id);
        this.debuff = debuff;
        this.atributo = atributo;
    }

    // Getters
    public Debuff getDebuff() { return debuff; }
    public Atributo getAtributo() { return atributo; }
}
