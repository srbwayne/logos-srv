package com.josecjuniors.logossrv.core.habilidade.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Habilidade extends AbstractDomainAggregate<HabilidadeId> {

    @Column(nullable = false, unique = true)
    private String nome;

    @Lob
    private String descricao;

    @OneToMany(mappedBy = "habilidade", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<HabilidadeRequisito> requisitos = new HashSet<>();

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
    public Set<HabilidadeRequisito> getRequisitos() { return requisitos; }
}
