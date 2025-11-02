package com.josecjuniors.logossrv.core.util.domain.builder;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;

/**
 * Classe base abstrata para builders de entidades.
 *
 * @param <B> O tipo do Builder concreto (Curiously Recurring Template Pattern).
 * @param <E> O tipo da Entidade que está sendo construída.
 * @param <I> O tipo do ID da Entidade.
 */
public abstract class AbstractEntityBuilder<B extends AbstractEntityBuilder<B, E, I>, E extends AbstractDomainAggregate<I>, I extends DomainObjectId> {

    protected I id;

    /**
     * Define o ID da entidade.
     * @param id O ID da entidade.
     * @return A instância do builder concreto para encadeamento de métodos.
     */
    public B withId(I id) {
        this.id = id;
        return self();
    }

    /**
     * Constrói e retorna a entidade final.
     * @return A instância da entidade construída.
     * @throws IllegalStateException se o ID não for fornecido.
     */
    public E build() {
        if (id == null) {
            throw new IllegalStateException("O ID da entidade não pode ser nulo.");
        }
        return buildEntity();
    }

    /**
     * Método que deve ser implementado pelo builder concreto para construir a entidade.
     * @return A instância da entidade.
     */
    protected abstract E buildEntity();

    /**
     * Retorna a instância do builder concreto (`this`).
     * @return A instância do builder concreto.
     */
    protected abstract B self();
}
