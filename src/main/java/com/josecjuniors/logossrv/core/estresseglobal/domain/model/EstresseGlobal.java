package com.josecjuniors.logossrv.core.estresseglobal.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;

import jakarta.persistence.Entity;

@Entity
public class EstresseGlobal extends AbstractDomainAggregate<EstresseGlobalId> {

    private Integer pontuacaoAtual;

    // Construtor para JPA
    protected EstresseGlobal() {
        super();
    }

    public EstresseGlobal(EstresseGlobalId id, Integer pontuacaoAtual) {
        super(id);
        this.pontuacaoAtual = pontuacaoAtual;
    }

    public Integer getPontuacaoAtual() {
        return pontuacaoAtual;
    }

    public void setPontuacaoAtual(Integer pontuacaoAtual) {
        this.pontuacaoAtual = pontuacaoAtual;
    }
}
