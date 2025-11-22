package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "regra_distribuicao_habilidade")
public class RegraDistribuicaoHabilidade extends AbstractDomainAggregate<RegraDistribuicaoHabilidadeId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_id", nullable = false)
    private Atributo atributo;

    @Column(nullable = false)
    private Double pesoDistribuicao; // Ex: 0.7 para 70%

    protected RegraDistribuicaoHabilidade() {
        super();
    }

    public RegraDistribuicaoHabilidade(RegraDistribuicaoHabilidadeId id, Habilidade habilidade, Atributo atributo, Double pesoDistribuicao) {
        super(id);
        this.habilidade = habilidade;
        this.atributo = atributo;
        this.pesoDistribuicao = pesoDistribuicao;
    }

    public void atualizar(Double pesoDistribuicao) {
        if (pesoDistribuicao != null) {
            this.pesoDistribuicao = pesoDistribuicao;
        }
    }

    // Getters
    public Habilidade getHabilidade() { return habilidade; }
    public Atributo getAtributo() { return atributo; }
    public Double getPesoDistribuicao() { return pesoDistribuicao; }
}
