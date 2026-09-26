package com.josecjuniors.logossrv.core.progression.application.port.in;

import java.time.Instant;

public interface CreateProgressionSubjectLinkChallengeUseCase {
    ChallengeResult create(String authenticatedUserEmail, String namespace);

    record ChallengeResult(String namespace, String challengeToken, Instant expiresAt) { }
}
