package com.josecjuniors.logossrv.core.vicio.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Vicio extends AbstractDomainAggregate<VicioId> {

    @Column(nullable = false, unique = true)
    private String nome;

    protected Vicio() {
        super();
    }

    public Vicio(VicioId id, String nome) {
        super(id);
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
