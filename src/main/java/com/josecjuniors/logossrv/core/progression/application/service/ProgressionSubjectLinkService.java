package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.progression.application.port.in.ConfirmExternalSubjectIdentityUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.in.CreateProgressionSubjectLinkChallengeUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionSubjectLinkChallengeStore;
import com.josecjuniors.logossrv.core.progression.application.port.out.VerifiedExternalSubjectIdentityPort;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectLinkChallengeInvalidException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.config.progression.ProgressionSubjectLinkProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class ProgressionSubjectLinkService implements CreateProgressionSubjectLinkChallengeUseCase,
        ConfirmExternalSubjectIdentityUseCase {
    private static final String LOGOS_NATIVE = "logos-native";
    private final JogadorRepository jogadorRepository;
    private final ProgressionSubjectLinkChallengeStore challengeStore;
    private final VerifiedExternalSubjectIdentityPort identities;
    private final ProgressionSubjectLinkProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public ProgressionSubjectLinkService(JogadorRepository jogadorRepository,
                                         ProgressionSubjectLinkChallengeStore challengeStore,
                                         VerifiedExternalSubjectIdentityPort identities,
                                         ProgressionSubjectLinkProperties properties,
                                         Clock clock) {
        this.jogadorRepository = jogadorRepository;
        this.challengeStore = challengeStore;
        this.identities = identities;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ChallengeResult create(String authenticatedUserEmail, String rawNamespace) {
        String namespace = normalizeNamespace(rawNamespace);
        if (LOGOS_NATIVE.equals(namespace)) throw new IllegalArgumentException("logos-native namespace is managed by Logos");
        var jogador = jogadorRepository.findByUserEmail(authenticatedUserEmail)
                .orElseThrow(JogadorNaoEncontradoException::new);
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String hash = hash(token);
        Instant now = clock.instant();
        Instant expires = now.plus(properties.getChallengeTtl());
        challengeStore.create(hash, namespace, jogador.getId().getValue(), now, expires);
        return new ChallengeResult(namespace, token, expires);
    }

    @Override
    @Transactional
    public ExternalSubjectReference confirm(String rawNamespace, String rawExternalId,
                                            String challengeToken, String clientId) {
        ExternalSubjectReference reference = new ExternalSubjectReference(rawNamespace, rawExternalId);
        if (challengeToken == null || challengeToken.isBlank()) throw new ProgressionSubjectLinkChallengeInvalidException();
        var challenge = challengeStore.findForUpdate(hash(challengeToken))
                .orElseThrow(ProgressionSubjectLinkChallengeInvalidException::new);
        Instant now = clock.instant();
        if (!challenge.namespace().equals(reference.namespace())
                || challenge.consumedAt() != null
                || !challenge.expiresAt().isAfter(now)) {
            throw new ProgressionSubjectLinkChallengeInvalidException();
        }
        identities.confirm(reference.namespace(), reference.externalId(), challenge.jogadorId(), clientId, now);
        challengeStore.consume(challenge.id(), now);
        return reference;
    }

    private String normalizeNamespace(String raw) {
        return new ExternalSubjectReference(raw, "validation").namespace();
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
