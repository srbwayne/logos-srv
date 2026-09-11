package com.josecjuniors.logossrv.core.progressionconfiguration.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoNaoEncontradoException;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoNaoEncontradoException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.exception.ProgressionConfigurationAuthoringConflictException;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDefinition;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDraft;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.repository.ProgressionConfigurationAuthoringRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class ProgressionConfigurationAuthoringService {
    private final ProgressionConfigurationAuthoringRepository repository;
    private final FatorCalculoRepository factors;
    private final AtributoRepository attributes;

    public ProgressionConfigurationAuthoringService(ProgressionConfigurationAuthoringRepository repository,
                                                    FatorCalculoRepository factors,
                                                    AtributoRepository attributes) {
        this.repository = repository;
        this.factors = factors;
        this.attributes = attributes;
    }

    @Transactional
    public ProgressionConfigurationDefinition create(String rawKey) {
        String key = ExternalProgressionConfigurationReference.normalizeKey(rawKey);
        try {
            return repository.create(key);
        } catch (DataIntegrityViolationException exception) {
            throw new ProgressionConfigurationAuthoringConflictException("logicalKey already exists");
        }
    }

    @Transactional(readOnly = true)
    public ProgressionConfigurationDefinition get(String rawKey) {
        return repository.find(ExternalProgressionConfigurationReference.normalizeKey(rawKey))
                .orElseThrow(com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionConfigurationNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public ProgressionConfigurationDraft getDraft(String rawKey) {
        String key = ExternalProgressionConfigurationReference.normalizeKey(rawKey);
        ProgressionConfigurationDefinition definition = get(key);
        if (definition.legacyLinked()) {
            throw new ProgressionConfigurationAuthoringConflictException("legacy-linked configuration is not available to modern authoring");
        }
        return repository.findDraft(key)
                .orElseThrow(com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionConfigurationNotFoundException::new);
    }

    @Transactional
    public ProgressionConfigurationDraft replaceDraft(String rawKey, long expectedVersion, ProgressionConfigurationDraft draft) {
        String key = ExternalProgressionConfigurationReference.normalizeKey(rawKey);
        ProgressionConfigurationDefinition definition = get(key);
        if (definition.legacyLinked()) {
            throw new ProgressionConfigurationAuthoringConflictException("legacy-linked configuration is not available to modern authoring");
        }
        validateDraft(draft);
        try {
            return repository.replaceDraft(key, expectedVersion, draft);
        } catch (DataIntegrityViolationException exception) {
            throw new ProgressionConfigurationAuthoringConflictException("draft update conflicts with current state");
        }
    }

    @Transactional
    public ProgressionConfigurationAuthoringRepository.PublishedProgressionConfigurationVersion publish(
            String rawKey, long expectedDraftVersion) {
        String key = ExternalProgressionConfigurationReference.normalizeKey(rawKey);
        ProgressionConfigurationDefinition definition = get(key);
        if (definition.legacyLinked()) {
            throw new ProgressionConfigurationAuthoringConflictException("legacy-linked configuration is not available to modern authoring");
        }
        var existing = repository.findPublishedByDraftVersion(key, expectedDraftVersion);
        if (existing.isPresent()) return existing.get();
        ProgressionConfigurationDraft draft = repository.lockDraft(key, expectedDraftVersion);
        validatePublishableDraft(draft);
        return repository.publishLocked(key, expectedDraftVersion, draft);
    }

    private void validatePublishableDraft(ProgressionConfigurationDraft draft) {
        if (draft.baseXp() == null || draft.baseStress() == null) {
            throw new IllegalArgumentException("base XP and base stress are required for publication");
        }
        if (draft.factors().stream().anyMatch(key -> key == null || key.isBlank())) {
            throw new IllegalArgumentException("published factors require semantic keys");
        }
        for (ProgressionConfigurationDraft.Distribution distribution : draft.distributions()) {
            if (distribution.attributeId() == null) {
                throw new IllegalArgumentException("published distributions require an attribute");
            }
            for (ProgressionConfigurationDraft.XpRule rule : distribution.xpRules()) {
                if (rule.fact() == null || rule.fact().isBlank()) {
                    throw new IllegalArgumentException("published XP rules require a fact");
                }
                if (rule.calculationMode() == null) {
                    throw new IllegalArgumentException("published XP rules require a calculation mode");
                }
            }
            for (ProgressionConfigurationDraft.StressRule rule : distribution.stressRules()) {
                if (rule.multiplier() == null || rule.type() == null || rule.type().isBlank()) {
                    throw new IllegalArgumentException("published stress rules require multiplier and type");
                }
            }
        }
    }

    private void validateDraft(ProgressionConfigurationDraft draft) {
        Set<String> factorKeys = new HashSet<>();
        for (String key : draft.factors()) {
            FatorCalculo factor = factors.findBySemanticKey(ExternalProgressionConfigurationReference.normalizeKey(key))
                    .orElseThrow(() -> new FatorCalculoNaoEncontradoException(key));
            if (!factorKeys.add(factor.getSemanticKey())) {
                throw new IllegalArgumentException("duplicate factor in draft");
            }
        }
        for (ProgressionConfigurationDraft.Distribution distribution : draft.distributions()) {
            if (attributes.findById(new AtributoId(distribution.attributeId())).isEmpty()) {
                throw new AtributoNaoEncontradoException();
            }
            for (ProgressionConfigurationDraft.XpRule rule : distribution.xpRules()) {
                String key = ExternalProgressionConfigurationReference.normalizeKey(rule.fact());
                if (!factorKeys.contains(key)) {
                    throw new IllegalArgumentException("XP rule fact must be listed in factors");
                }
                if (factors.findBySemanticKey(key).isEmpty()) {
                    throw new FatorCalculoNaoEncontradoException(key);
                }
            }
        }
    }
}
