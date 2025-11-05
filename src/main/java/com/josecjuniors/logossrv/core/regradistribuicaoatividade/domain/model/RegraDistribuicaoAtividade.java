package com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "regra_distribuicao_atividade")
public class RegraDistribuicaoAtividade extends AbstractDomainAggregate<RegraDistribuicaoAtividadeId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_config_id", nullable = false)
    private AtividadeConfig atividadeConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_id", nullable = false)
    private Atributo atributo;

    @Column(nullable = false)
    private Double pesoPercentual;

    protected RegraDistribuicaoAtividade() {
        super();
    }

    public RegraDistribuicaoAtividade(RegraDistribuicaoAtividadeId id, AtividadeConfig atividadeConfig, Atributo atributo, Double pesoPercentual) {
        super(id);
        this.atividadeConfig = atividadeConfig;
        this.atributo = atributo;
        this.pesoPercentual = pesoPercentual;
    }

    public AtividadeConfig getAtividadeConfig() { return atividadeConfig; }
    public Atributo getAtributo() { return atributo; }
    public Double getPesoPercentual() { return pesoPercentual; }
}
