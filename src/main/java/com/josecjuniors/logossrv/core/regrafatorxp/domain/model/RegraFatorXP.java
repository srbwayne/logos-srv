package com.josecjuniors.logossrv.core.regrafatorxp.domain.model;

import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

@Entity
@Table(name = "regra_fator_xp")
public class RegraFatorXP extends AbstractDomainAggregate<RegraFatorXPId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regra_distribuicao_atividade_id", nullable = false)
    private RegraDistribuicaoAtividade regraDistribuicaoAtividade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fator_calculo_id", nullable = false)
    private FatorCalculo fatorCalculo;

    @Column(nullable = false)
    private Double pesoMultiplicador;

    @Column(nullable = false)
    private Double pontoCorteMin;

    protected RegraFatorXP() {
        super();
    }

    public RegraFatorXP(RegraFatorXPId id, RegraDistribuicaoAtividade regraDistribuicaoAtividade, FatorCalculo fatorCalculo, Double pesoMultiplicador, Double pontoCorteMin) {
        super(id);
        this.regraDistribuicaoAtividade = regraDistribuicaoAtividade;
        this.fatorCalculo = fatorCalculo;
        this.pesoMultiplicador = pesoMultiplicador;
        this.pontoCorteMin = pontoCorteMin;
    }

    // Getters
    public RegraDistribuicaoAtividade getRegraDistribuicaoAtividade() { return regraDistribuicaoAtividade; }
    public FatorCalculo getFatorCalculo() { return fatorCalculo; }
    public Double getPesoMultiplicador() { return pesoMultiplicador; }
    public Double getPontoCorteMin() { return pontoCorteMin; }
}
