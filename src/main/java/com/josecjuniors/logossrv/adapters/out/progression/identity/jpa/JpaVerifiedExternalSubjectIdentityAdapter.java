package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.progression.application.port.out.VerifiedExternalSubjectIdentityPort;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectIdentityConflictException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class JpaVerifiedExternalSubjectIdentityAdapter implements VerifiedExternalSubjectIdentityPort {
    private final ProgressionSubjectIdentityJpaRepository repository;

    public JpaVerifiedExternalSubjectIdentityAdapter(ProgressionSubjectIdentityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void confirm(String namespace, String externalId, UUID jogadorId, String clientId, Instant verifiedAt) {
        var current = repository.findByNamespaceAndExternalId(namespace, externalId);
        if (current.isEmpty()) {
            repository.insertVerifiedIfAbsent(UUID.randomUUID(), namespace, externalId, jogadorId, verifiedAt, clientId);
        } else if ("UNVERIFIED".equals(current.get().getVerificationStatus())) {
            repository.promoteUnverified(namespace, externalId, jogadorId, verifiedAt, clientId);
        }
        var result = repository.findByNamespaceAndExternalId(namespace, externalId).orElseThrow();
        if (!"INTEGRATION_VERIFIED".equals(result.getVerificationStatus())
                || !result.getJogador().getId().getValue().equals(jogadorId)) {
            throw new ProgressionSubjectIdentityConflictException();
        }
    }
}
