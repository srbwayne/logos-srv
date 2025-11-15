package com.josecjuniors.logossrv.core.regrafatorestresse.domain.model;

import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "regra_fator_estresse")
public class RegraFatorEstresse extends AbstractDomainAggregate<RegraFatorEstresseId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regra_distribuicao_atividade_id", nullable = false)
    private RegraDistribuicaoAtividade regraDistribuicaoAtividade;

    @Column(nullable = false)
    private Double pesoMultiplicador;

    @Column(nullable = false)
    private Double pontoCorteMin;

    private Double pontoCorteMax; // Pode ser nulo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoFatorEstresse tipo;

    protected RegraFatorEstresse() {
        super();
    }

    public RegraFatorEstresse(RegraFatorEstresseId id, RegraDistribuicaoAtividade regraDistribuicaoAtividade, Double pesoMultiplicador, Double pontoCorteMin, Double pontoCorteMax, TipoFatorEstresse tipo) {
        super(id);
        this.regraDistribuicaoAtividade = regraDistribuicaoAtividade;
        this.pesoMultiplicador = pesoMultiplicador;
        this.pontoCorteMin = pontoCorteMin;
        this.pontoCorteMax = pontoCorteMax;
        this.tipo = tipo;
    }

    public void atualizar(Double pesoMultiplicador, Double pontoCorteMin, Double pontoCorteMax, TipoFatorEstresse tipo) {
        this.pesoMultiplicador = pesoMultiplicador;
        this.pontoCorteMin = pontoCorteMin;
        this.pontoCorteMax = pontoCorteMax;
        this.tipo = tipo;
    }

    // Getters
    public RegraDistribuicaoAtividade getRegraDistribuicaoAtividade() { return regraDistribuicaoAtividade; }
    public Double getPesoMultiplicador() { return pesoMultiplicador; }
    public Double getPontoCorteMin() { return pontoCorteMin; }
    public Double getPontoCorteMax() { return pontoCorteMax; }
    public TipoFatorEstresse getTipo() { return tipo; }
}
