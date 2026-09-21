package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProgressionOutcomeAttributeSemanticSnapshotter {
    private final AtributoRepository atributos;

    public ProgressionOutcomeAttributeSemanticSnapshotter(AtributoRepository atributos) {
        this.atributos = atributos;
    }

    public ProgressionOutcome snapshot(ProgressionOutcome outcome) {
        Set<String> keys = new HashSet<>();
        if (outcome.result() != null) {
            outcome.result().attributeProgressions().forEach(value -> keys.add(value.attributeKey()));
        }
        if (outcome.updatedProfile() != null) {
            outcome.updatedProfile().attributes().forEach(value -> keys.add(value.key()));
        }
        if (keys.isEmpty()) {
            return new ProgressionOutcome(outcome.result(), outcome.updatedProfile(), Map.of());
        }
        Map<String, String> semanticKeys = atributos.findAll().stream()
                .filter(attribute -> keys.contains(attribute.getId().getValue().toString()))
                .filter(attribute -> attribute.getSemanticKey() != null)
                .collect(Collectors.toMap(attribute -> attribute.getId().getValue().toString(),
                        Atributo::getSemanticKey, (first, ignored) -> first));
        return new ProgressionOutcome(outcome.result(), outcome.updatedProfile(), semanticKeys);
    }
}
