package com.josecjuniors.logossrv.core.security.workload.domain;

import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Registered trust material only; this does not represent an authenticated caller. */
public record TrustedWorkloadCredential(
        UUID principalDatabaseId,
        UUID credentialDatabaseId,
        WorkloadIssuer issuer,
        WorkloadPrincipalId principalId,
        WorkloadPrincipalLifecycle principalLifecycle,
        Instant principalDisabledAt,
        Instant principalRevokedAt,
        WorkloadKeyId kid,
        WorkloadSignatureAlgorithm algorithm,
        ECPublicKey publicKey,
        WorkloadCredentialLifecycle credentialLifecycle,
        Instant notBefore,
        Instant notAfter,
        Instant activatedAt,
        Instant revokedAt,
        Instant retiredAt) {

    public TrustedWorkloadCredential {
        Objects.requireNonNull(principalDatabaseId, "principalDatabaseId");
        Objects.requireNonNull(credentialDatabaseId, "credentialDatabaseId");
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(principalId, "principalId");
        Objects.requireNonNull(principalLifecycle, "principalLifecycle");
        Objects.requireNonNull(kid, "kid");
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(publicKey, "publicKey");
        Objects.requireNonNull(credentialLifecycle, "credentialLifecycle");
        Objects.requireNonNull(notBefore, "notBefore");
    }
}
