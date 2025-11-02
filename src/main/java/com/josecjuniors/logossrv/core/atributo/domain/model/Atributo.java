package com.josecjuniors.logossrv.core.atributo.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

@Entity
public class Atributo extends AbstractDomainAggregate<AtributoId> {

    @Column(nullable = false)
    private String nome;


    protected Atributo() {
        super();
        this.nome = null;    // Inicialização para final, exigida pelo JPA
    }

    public Atributo(AtributoId id, String nome) {
        super(id);
        this.nome = nome;

    }

    public String getNome() {
        return nome;
    }

    public void atualizarNome(String novoNome) {
        this.nome = novoNome;
    }
}
