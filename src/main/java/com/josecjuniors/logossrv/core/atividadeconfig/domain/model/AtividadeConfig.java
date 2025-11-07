package com.josecjuniors.logossrv.core.atividadeconfig.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Entity;

@Entity
public class AtividadeConfig extends AbstractDomainAggregate<AtividadeConfigId> {

    private String nome;

    private Integer xpBase;

    private Integer estresseBase;

    // Campos para a futura funcionalidade de penalidade por inatividade
    private Integer diasParaPenalidade; // Ex: 7 (dias)

    private Integer xpPerdaPorCiclo;    // Ex: -10 (XP)

    // Construtor para JPA
    protected AtividadeConfig() {
        super();
    }

    public AtividadeConfig(AtividadeConfigId id, String nome, Integer xpBase, Integer estresseBase) {
        super(id);
        this.nome = nome;
        this.xpBase = xpBase;
        this.estresseBase = estresseBase;
    }

    // Getters
    public String getNome() { return nome; }
    public Integer getXpBase() { return xpBase; }
    public Integer getEstresseBase() { return estresseBase; }
    public Integer getDiasParaPenalidade() { return diasParaPenalidade; }
    public Integer getXpPerdaPorCiclo() { return xpPerdaPorCiclo; }
}
