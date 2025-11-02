package com.josecjuniors.logossrv.core.atividadeconfig.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Entity;

@Entity
public class AtividadeConfig extends AbstractDomainAggregate<AtividadeConfigId> {

    private String nome;

    private Integer xpBase;

    private Integer estresseBase;


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

    public String getNome() {
        return nome;
    }

    public Integer getXpBase() {
        return xpBase;
    }

    public Integer getEstresseBase() {
        return estresseBase;
    }
}
