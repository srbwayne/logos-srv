package com.josecjuniors.logossrv.adapters.in.web.progression.dto.response;

import com.josecjuniors.logossrv.core.progression.application.port.in.CreateProgressionSubjectLinkChallengeUseCase;

import java.time.Instant;

public record ProgressionSubjectLinkChallengeResponse(String namespace, String challengeToken, Instant expiresAt) {
    public static ProgressionSubjectLinkChallengeResponse from(
            CreateProgressionSubjectLinkChallengeUseCase.ChallengeResult challenge) {
        return new ProgressionSubjectLinkChallengeResponse(challenge.namespace(), challenge.challengeToken(), challenge.expiresAt());
    }
}
