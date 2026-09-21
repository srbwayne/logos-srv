package com.josecjuniors.logossrv.core.atributo.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoSemanticKeyJaAtribuidaException;

@Entity
public class Atributo extends AbstractDomainAggregate<AtributoId> {

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "semantic_key", length = 64, unique = true)
    private String semanticKey;

    protected Atributo() {
        super();
    }

    public Atributo(AtributoId id, String nome, String descricao) {
        this(id, nome, descricao, null);
    }

    public Atributo(AtributoId id, String nome, String descricao, String semanticKey) {
        super(id);
        this.nome = nome;
        this.descricao = descricao;
        this.semanticKey = semanticKey == null ? null : AtributoSemanticKey.of(semanticKey).value();
    }

    public void atualizarNome(String novoNome) {
        if (novoNome != null && !novoNome.isBlank()) {
            this.nome = novoNome;
        }
    }

    public void atualizarDescricao(String novaDescricao) {
        this.descricao = novaDescricao;
    }

    public void assignSemanticKey(String semanticKey) {
        String normalized = AtributoSemanticKey.of(semanticKey).value();
        if (this.semanticKey != null && !this.semanticKey.equals(normalized)) {
            throw new AtributoSemanticKeyJaAtribuidaException();
        }
        this.semanticKey = normalized;
    }

    // Getters
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getSemanticKey() { return semanticKey; }
}
