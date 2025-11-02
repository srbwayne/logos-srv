package com.josecjuniors.logossrv.core.util.domain;

import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class DomainObjectId implements Serializable {

    private UUID value;

    protected DomainObjectId() {
        this.value = UUID.randomUUID();
    }

    protected DomainObjectId(UUID value) {
        this.value = value;
    }


    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainObjectId that = (DomainObjectId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
