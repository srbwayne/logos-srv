package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;

import java.util.List;

/** Resultado interno dos cálculos, sem transportar entidades JPA ou contratos REST. */
public record ProgressionResult(long xpGlobal, double stressTotal, List<AttributeProgression> attributeProgressions) {

    public record AttributeProgression(Atributo atributo, long xp) {
    }
}
