package com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @OneToMany(mappedBy = "regraDistribuicaoAtividade", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RegraFatorXP> regraFatorXPS = new HashSet<>();

    @OneToMany(mappedBy = "regraDistribuicaoAtividade", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RegraFatorEstresse> regraFatorEstresses = new HashSet<>();

    protected RegraDistribuicaoAtividade() {
        super();
    }

    public RegraDistribuicaoAtividade(RegraDistribuicaoAtividadeId id, AtividadeConfig atividadeConfig, Atributo atributo, Double pesoPercentual) {
        super(id);
        this.atividadeConfig = atividadeConfig;
        this.atributo = atributo;
        this.pesoPercentual = pesoPercentual;
    }

    public void atualizarPeso(Double novoPeso) {
        if (novoPeso != null && novoPeso >= 0) {
            this.pesoPercentual = novoPeso;
        }
    }

    // Getters
    public AtividadeConfig getAtividadeConfig() {
        return atividadeConfig;
    }

    public Atributo getAtributo() {
        return atributo;
    }

    public Double getPesoPercentual() {
        return pesoPercentual;
    }

    public Set<RegraFatorXP> getRegraFatorXPS() {
        return regraFatorXPS;
    }

    public void adicionarRegraFatorXPS(RegraFatorXP regraFatorXPS) {
        this.regraFatorXPS.add(regraFatorXPS);
    }

    public Set<RegraFatorEstresse> getRegraFatorEstresses() {
        return regraFatorEstresses;
    }

    public void adicionarRegraFatorEstresses(RegraFatorEstresse regraFatorEstresses) {
        this.regraFatorEstresses.add(regraFatorEstresses);
    }
}
