package com.josecjuniors.logossrv.core.util.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractDomainAggregate<ID extends DomainObjectId> {

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "id"))
    private ID id;

    protected AbstractDomainAggregate() {
        // Construtor padrão para JPA
    }

    protected AbstractDomainAggregate(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }
}
