package com.josecjuniors.logossrv.core.debuff.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class DebuffId extends DomainObjectId {
    public DebuffId() { super(); }
    public DebuffId(UUID value) { super(value); }
    public static DebuffId generate() { return new DebuffId(UUID.randomUUID()); }
}
