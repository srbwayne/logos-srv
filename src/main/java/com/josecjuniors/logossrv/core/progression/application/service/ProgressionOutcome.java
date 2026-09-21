package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;

import java.util.Map;
import java.util.Collections;

/** Resultado do caso de uso: cálculo bruto e estado atualizado para o chamador aplicar. */
public record ProgressionOutcome(ProgressionResult result, ProgressionProfile updatedProfile,
                                 Map<String, String> attributeSemanticKeys) {
    public ProgressionOutcome(ProgressionResult result, ProgressionProfile updatedProfile) {
        this(result, updatedProfile, Map.of());
    }

    public ProgressionOutcome {
        attributeSemanticKeys = attributeSemanticKeys == null || attributeSemanticKeys.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(Map.copyOf(attributeSemanticKeys));
    }
}
