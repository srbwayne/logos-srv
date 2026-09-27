package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Signed assertion with valid profile, claims, and trust lifecycle. Replay has NOT been consumed;
 * this is not a fully authenticated principal. F1D must consume replay before authentication completes.
 */
public record VerifiedWorkloadAssertion(
        WorkloadIssuer issuer,
        WorkloadPrincipalId principalId,
        WorkloadKeyId kid,
        UUID jti,
        Instant issuedAt,
        Instant expiresAt) {

    public VerifiedWorkloadAssertion {
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(principalId, "principalId");
        Objects.requireNonNull(kid, "kid");
        Objects.requireNonNull(jti, "jti");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
