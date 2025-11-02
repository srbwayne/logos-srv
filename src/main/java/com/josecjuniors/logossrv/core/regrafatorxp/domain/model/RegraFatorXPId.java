package com.josecjuniors.logossrv.core.regrafatorxp.domain.model;

import com.josecjuniors.logossrv.core.util.domain.DomainObjectId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class RegraFatorXPId extends DomainObjectId {
    public RegraFatorXPId() {
        super();
    }

    public RegraFatorXPId(UUID value) {
        super(value);
    }
}
