package com.josecjuniors.logossrv.core.progression.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProgressionSubjectLinkChallengeStore {
    void create(String tokenHash, String namespace, UUID jogadorId, Instant createdAt, Instant expiresAt);
    Optional<Challenge> findForUpdate(String tokenHash);
    void consume(UUID id, Instant consumedAt);

    record Challenge(UUID id, String tokenHash, String namespace, UUID jogadorId,
                     Instant expiresAt, Instant consumedAt) { }
}
