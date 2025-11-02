package com.josecjuniors.logossrv.core.atributo.domain.model;

import com.josecjuniors.logossrv.core.util.domain.builder.AbstractEntityBuilder;

public class AtributoBuilder extends AbstractEntityBuilder<AtributoBuilder, Atributo, AtributoId> {

    private String nome;


    public AtributoBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }


    @Override
    protected Atributo buildEntity() {
        if (nome == null || nome.isBlank()) {
            throw new IllegalStateException("Nome é obrigatório para construir um Atributo.");
        }
        // Chama o construtor package-private da entidade Atributo
        return new Atributo(this.id, this.nome);
    }

    @Override
    protected AtributoBuilder self() {
        return this;
    }
}
