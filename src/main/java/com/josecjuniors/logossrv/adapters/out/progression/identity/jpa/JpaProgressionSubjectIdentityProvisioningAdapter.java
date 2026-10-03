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
        UUID identityId = UUID.randomUUID();
        int inserted = repository.insertIfAbsent(identityId, reference.namespace(), reference.externalId(),
                jogador.getId().getValue(), classification.identityClass().name(),
                classification.ownershipStatus().name(), classification.verificationStatus().name());

        repository.findAppUserIdByNamespaceAndExternalId(reference.namespace(), reference.externalId())
                .filter(owner -> owner.getValue().equals(target.value()))
                .orElseThrow(ProgressionSubjectIdentityConflictException::new);

        if (inserted == 1) {
            UUID actionActorId = logosNative ? null : target.value();
            historyRepository.saveAndFlush(ProgressionSubjectOwnershipHistoryEntry.initial(
                    identityId, jogador.getId().getValue(), classification, actionActorId, Instant.now()));
        }
    }
}
