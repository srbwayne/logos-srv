package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionSubjectLinkChallengeStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaProgressionSubjectLinkChallengeStore implements ProgressionSubjectLinkChallengeStore {
    private final ProgressionSubjectLinkChallengeJpaRepository repository;

    public JpaProgressionSubjectLinkChallengeStore(ProgressionSubjectLinkChallengeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void create(String tokenHash, String namespace, UUID jogadorId, Instant createdAt, Instant expiresAt) {
        repository.saveAndFlush(new ProgressionSubjectLinkChallengeEntity(UUID.randomUUID(), tokenHash, namespace,
                jogadorId, createdAt, expiresAt));
    }

    @Override
    public Optional<Challenge> findForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(entity -> new Challenge(entity.getId(),
                entity.getTokenHash(), entity.getNamespace(), entity.getJogadorId(), entity.getExpiresAt(), entity.getConsumedAt()));
    }

    @Override
    public void consume(UUID id, Instant consumedAt) {
        var challenge = repository.findById(id).orElseThrow();
        challenge.consume(consumedAt);
        repository.saveAndFlush(challenge);
    }
}
