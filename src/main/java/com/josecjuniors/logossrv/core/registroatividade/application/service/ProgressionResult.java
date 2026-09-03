package com.josecjuniors.logossrv.core.registroatividade.application.service;

import java.util.List;

/** Resultado interno dos cálculos, sem transportar entidades JPA ou contratos REST. */
public record ProgressionResult(long xpGlobal, double stressTotal, List<AttributeProgression> attributeProgressions) {

    public ProgressionResult {
        attributeProgressions = List.copyOf(attributeProgressions);
    }

    public record AttributeProgression(String attributeKey, long xp) {
    }
}
