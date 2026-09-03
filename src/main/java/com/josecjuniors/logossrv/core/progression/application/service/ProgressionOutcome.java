package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;

/** Resultado do caso de uso: cálculo bruto e estado atualizado para o chamador aplicar. */
public record ProgressionOutcome(ProgressionResult result, ProgressionProfile updatedProfile) {
}
