package com.josecjuniors.logossrv.core.habilidade.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

@Entity
public class Habilidade extends AbstractDomainAggregate<HabilidadeId> {

    @Column(nullable = false, unique = true)
    private String nome;

    @Lob
    private String descricao;

    protected Habilidade() {
        super();
    }

    public Habilidade(HabilidadeId id, String nome, String descricao) {
        super(id);
        this.nome = nome;
        this.descricao = descricao;
    }

    public void atualizarNome(String novoNome) {
        if (novoNome != null && !novoNome.isBlank()) {
            this.nome = novoNome;
        }
    }

    public void atualizarDescricao(String novaDescricao) {
        this.descricao = novaDescricao;
    }

    // Getters
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
}
