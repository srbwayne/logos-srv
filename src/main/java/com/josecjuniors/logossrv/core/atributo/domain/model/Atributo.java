package com.josecjuniors.logossrv.core.atributo.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

@Entity
public class Atributo extends AbstractDomainAggregate<AtributoId> {

    @Column(nullable = false, unique = true)
    private String nome;

    @Lob
    private String descricao;

    protected Atributo() {
        super();
    }

    public Atributo(AtributoId id, String nome, String descricao) {
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
