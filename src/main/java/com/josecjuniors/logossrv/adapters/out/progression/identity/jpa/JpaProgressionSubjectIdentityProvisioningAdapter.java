package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.adapters.out.subjectownership.jpa.ProgressionSubjectOwnershipHistoryEntry;
import com.josecjuniors.logossrv.adapters.out.subjectownership.jpa.ProgressionSubjectOwnershipHistoryJpaRepository;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionSubjectIdentityProvisioningPort;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectIdentityConflictException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipClassification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

@Component
public class JpaProgressionSubjectIdentityProvisioningAdapter
        implements ProgressionSubjectIdentityProvisioningPort {

    private final ProgressionSubjectIdentityJpaRepository repository;
    private final ProgressionSubjectOwnershipHistoryJpaRepository historyRepository;
    private final JogadorRepository jogadorRepository;

    public JpaProgressionSubjectIdentityProvisioningAdapter(
            ProgressionSubjectIdentityJpaRepository repository,
            ProgressionSubjectOwnershipHistoryJpaRepository historyRepository,
            JogadorRepository jogadorRepository) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.jogadorRepository = jogadorRepository;
    }

    @Override
    @Transactional
    public void provision(ExternalSubjectReference reference, SubjectId target) {
        var jogador = jogadorRepository.findByAppUserId(new AppUserId(target.value()))
                .orElseThrow(JogadorNaoEncontradoException::new);
        boolean logosNative = "logos-native".equals(reference.namespace());
        var classification = logosNative
                ? SubjectOwnershipClassification.nativeRegistration()
                : SubjectOwnershipClassification.pocSelfLink();
        var currentIdentityId = repository.lockCurrentIdentityId(reference.namespace(), reference.externalId());
        if (currentIdentityId.isPresent()) {
            ensureSameCurrentTarget(reference, target, currentIdentityId.get());
            return;
        }
        if (repository.existsByNamespaceAndExternalId(reference.namespace(), reference.externalId())) {
            var concurrentlyProvisionedIdentityId = repository.lockCurrentIdentityId(
                    reference.namespace(), reference.externalId());
            if (concurrentlyProvisionedIdentityId.isPresent()) {
                ensureSameCurrentTarget(reference, target, concurrentlyProvisionedIdentityId.get());
                return;
            }
            throw new DataIntegrityViolationException(
                    "Subject identity history exists without a current-binding pointer");
        }

        UUID identityId = UUID.randomUUID();
        int claimed = repository.insertCurrentBindingIfAbsent(
                reference.namespace(), reference.externalId(), identityId);
        if (claimed == 0) {
            var winner = repository.lockCurrentIdentityId(reference.namespace(), reference.externalId())
                    .orElseThrow(() -> new DataIntegrityViolationException(
                            "Current-binding pointer claim was lost without a visible winner"));
            ensureSameCurrentTarget(reference, target, winner);
            return;
        }

        repository.insertIdentity(identityId, reference.namespace(), reference.externalId(),
                jogador.getId().getValue(), classification.identityClass().name(),
                classification.ownershipStatus().name(), classification.verificationStatus().name());
        UUID actionActorId = logosNative ? null : target.value();
        historyRepository.saveAndFlush(ProgressionSubjectOwnershipHistoryEntry.initial(
                identityId, jogador.getId().getValue(), classification, actionActorId, Instant.now()));
    }

    private void ensureSameCurrentTarget(ExternalSubjectReference reference, SubjectId target,
                                         UUID currentIdentityId) {
        var current = repository.findCurrentIdentityByIdForUpdate(currentIdentityId)
                .orElseThrow(() -> new DataIntegrityViolationException(
                        "Current-binding pointer references a missing identity"));
        if (!current.getNamespace().equals(reference.namespace())
                || !current.getExternalId().equals(reference.externalId())) {
            throw new DataIntegrityViolationException(
                    "Current-binding pointer locator does not match identity locator");
        }
        if (!current.getJogador().getUser().getId().getValue().equals(target.value())) {
            throw new ProgressionSubjectIdentityConflictException();
        }
    }
}
