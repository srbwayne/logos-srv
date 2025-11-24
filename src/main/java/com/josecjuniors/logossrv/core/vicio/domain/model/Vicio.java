package com.josecjuniors.logossrv.core.vicio.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Vicio extends AbstractDomainAggregate<VicioId> {

    @Column(nullable = false, unique = true)
    private String nome;

    @Lob
    private String descricao;

    @OneToMany(mappedBy = "vicio", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RegraVicio> regras = new HashSet<>();

    protected Vicio() {
        super();
    }

    public Vicio(VicioId id, String nome, String descricao) {
        super(id);
        this.nome = nome;
        this.descricao = descricao;
    }

    public void atualizar(String nome, String descricao) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome;
        }
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Set<RegraVicio> getRegras() {
        return regras;
    }
}
