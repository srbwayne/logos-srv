package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Identidade opaca do dono do estado de progressão no limite atual do Logos. */
public record SubjectId(UUID value) {

    public SubjectId {
        Objects.requireNonNull(value, "subjectId");
    }
}
