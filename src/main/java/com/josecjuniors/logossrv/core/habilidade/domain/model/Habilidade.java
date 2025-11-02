package com.josecjuniors.logossrv.core.habilidade.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Habilidade extends AbstractDomainAggregate<HabilidadeId> {

    @Column(nullable = false, unique = true)
    private String nome;

    protected Habilidade() {
        super();
    }

    public Habilidade(HabilidadeId id, String nome) {
        super(id);
        this.nome = nome;
    }

    public void atualizarNome(String novoNome) {
        if (novoNome != null && !novoNome.isBlank()) {
            this.nome = novoNome;
        }
    }

    // Getter
    public String getNome() { return nome; }
}
