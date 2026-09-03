package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.List;

/** Fatos observados, sem regras ou estado atual de progressão. */
public record ProgressionFact(List<Detail> details) {

    public ProgressionFact {
        details = List.copyOf(details);
    }

    public record Detail(String factorKey, double value) {
    }
}
