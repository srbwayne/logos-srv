package com.josecjuniors.logossrv.core.atividadeconfig.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

@Entity
public class AtividadeConfig extends AbstractDomainAggregate<AtividadeConfigId> {

    private String nome;
    @Lob
    private String descricao;
    private Integer xpBase;
    private Integer estresseBase;
    private Integer diasParaPenalidade;
    private Integer xpPerdaPorCiclo;

    protected AtividadeConfig() {
        super();
    }

    public AtividadeConfig(AtividadeConfigId id, String nome, String descricao, Integer xpBase, Integer estresseBase, Integer diasParaPenalidade, Integer xpPerdaPorCiclo) {
        super(id);
        this.nome = nome;
        this.descricao = descricao;
        this.xpBase = xpBase;
        this.estresseBase = estresseBase;
        this.diasParaPenalidade = diasParaPenalidade;
        this.xpPerdaPorCiclo = xpPerdaPorCiclo;
    }

    public void atualizar(String nome, String descricao, Integer xpBase, Integer estresseBase, Integer diasParaPenalidade, Integer xpPerdaPorCiclo) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome;
        }
        this.descricao = descricao;
        this.xpBase = xpBase;
        this.estresseBase = estresseBase;
        this.diasParaPenalidade = diasParaPenalidade;
        this.xpPerdaPorCiclo = xpPerdaPorCiclo;
    }

    // Getters
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Integer getXpBase() { return xpBase; }
    public Integer getEstresseBase() { return estresseBase; }
    public Integer getDiasParaPenalidade() { return diasParaPenalidade; }
    public Integer getXpPerdaPorCiclo() { return xpPerdaPorCiclo; }
}
