package com.josecjuniors.logossrv.core.fatorcalculo.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Entity;

/**
 * Representa uma métrica que pode ser usada para registrar uma atividade.
 * Ex: Tempo Total (min), Distância (km), Nível de Determinação (1-5).
 */
@Entity
public class FatorCalculo extends AbstractDomainAggregate<FatorCalculoId> {

    private String nome;

    private String unidadeMedida; // Ex: "min", "km", "bpm", "1-5"

    protected FatorCalculo() {
        super();
    }

    public FatorCalculo(FatorCalculoId id, String nome, String unidadeMedida) {
        super(id);
        this.nome = nome;
        this.unidadeMedida = unidadeMedida;
    }

    public String getNome() {
        return nome;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }
}