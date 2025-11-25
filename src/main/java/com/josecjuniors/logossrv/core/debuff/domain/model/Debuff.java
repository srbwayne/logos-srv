package com.josecjuniors.logossrv.core.debuff.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "debuff")
public class Debuff extends AbstractDomainAggregate<DebuffId> {

    @Column(nullable = false, unique = true)
    private String nome;

    protected Debuff() {
        super();
    }

    public Debuff(DebuffId id, String nome) {
        super(id);
        this.nome = nome;
    }

    public void atualizar(String nome) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome;
        }
    }

    public String getNome() {
        return nome;
    }
}
