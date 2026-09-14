package com.josecjuniors.logossrv.core.atividadeconfig.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "atividade_config")
public class AtividadeConfig extends AbstractDomainAggregate<AtividadeConfigId> {
    private String nome;
    @Lob
    private String descricao;

    protected AtividadeConfig() { super(); }

    public AtividadeConfig(AtividadeConfigId id, String nome, String descricao) {
        super(id);
        this.nome = nome;
        this.descricao = descricao;
    }

    public void atualizarCatalogo(String nome, String descricao) {
        if (nome != null && !nome.isBlank()) this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
}
