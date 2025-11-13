package com.josecjuniors.logossrv.core.fatorcalculo.domain.model;

import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class FatorCalculo extends AbstractDomainAggregate<FatorCalculoId> {

    @Column(nullable = false, unique = true)
    private String nome;

    private String unidadeMedida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInput tipoInput;

    protected FatorCalculo() {
        super();
    }

    public FatorCalculo(FatorCalculoId id, String nome, String unidadeMedida, TipoInput tipoInput) {
        super(id);
        this.nome = nome;
        this.unidadeMedida = unidadeMedida;
        this.tipoInput = tipoInput;
    }

    public void atualizar(String nome, String unidadeMedida, TipoInput tipoInput) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome;
        }
        this.unidadeMedida = unidadeMedida;
        if (tipoInput != null) {
            this.tipoInput = tipoInput;
        }
    }

    // Getters
    public String getNome() { return nome; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public TipoInput getTipoInput() { return tipoInput; }
}
