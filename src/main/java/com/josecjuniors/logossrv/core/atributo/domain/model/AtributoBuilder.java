package com.josecjuniors.logossrv.core.atributo.domain.model;

import com.josecjuniors.logossrv.core.util.domain.builder.AbstractEntityBuilder;

public class AtributoBuilder extends AbstractEntityBuilder<AtributoBuilder, Atributo, AtributoId> {

    private String nome;
    private String descricao;



    public AtributoBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public AtributoBuilder withDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }


    @Override
    protected Atributo buildEntity() {
        if (nome == null || nome.isBlank()) {
            throw new IllegalStateException("Nome é obrigatório para construir um Atributo.");
        }
        // Chama o construtor package-private da entidade Atributo
        return new Atributo(this.id, this.nome, this.descricao);
    }

    @Override
    protected AtributoBuilder self() {
        return this;
    }
}
