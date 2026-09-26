package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.workload.domain.TrustedWorkloadCredential;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadCredentialLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalLifecycle;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionFailureReason.*;

@Component
public class WorkloadAssertionClaimValidator {
    private static final Pattern UUID_V4 = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    private static final Set<String> CLAIMS = WorkloadAssertionProfile.CLAIM_FIELDS;
    public VerifiedWorkloadAssertion validate(Claims claims, TrustedWorkloadCredential credential, Instant now) {
        if (!claims.keySet().equals(CLAIMS)) throw failure(MALFORMED_ASSERTION);

        if (!WorkloadAssertionProfile.ISSUER.equals(claims.getIssuer())
                || !credential.issuer().value().equals(claims.getIssuer())) {
            throw failure(ISSUER_INVALID);
        }
        if (!WorkloadAssertionProfile.SUBJECT.equals(claims.getSubject())
                || !credential.principalId().value().equals(claims.getSubject())) {
            throw failure(SUBJECT_INVALID);
        }
        if (!Set.of(WorkloadAssertionProfile.AUDIENCE).equals(claims.getAudience())) {
            throw failure(AUDIENCE_INVALID);
        }

        Instant issuedAt = instant(claims.getIssuedAt(), IAT_INVALID);
        Instant expiresAt = instant(claims.getExpiration(), EXP_INVALID);
        if (issuedAt.isAfter(now.plus(WorkloadAssertionProfile.CLOCK_SKEW))) throw failure(IAT_INVALID);
        if (!expiresAt.isAfter(issuedAt)
                || Duration.between(issuedAt, expiresAt).compareTo(WorkloadAssertionProfile.MAX_TTL) > 0
                || issuedAt.isBefore(now.minus(WorkloadAssertionProfile.MAX_AGE))) {
            throw failure(EXP_INVALID);
        }
        if (!now.isBefore(expiresAt.plus(WorkloadAssertionProfile.CLOCK_SKEW))) throw failure(EXP_INVALID);

        String tokenId = claims.getId();
        if (tokenId == null || !UUID_V4.matcher(tokenId).matches()) throw failure(JTI_INVALID);
        UUID jti;
        try {
            jti = UUID.fromString(tokenId);
        } catch (IllegalArgumentException exception) {
            throw failure(JTI_INVALID);
        }

        validateLifecycle(credential, now);
        return new VerifiedWorkloadAssertion(credential.issuer(), credential.principalId(), credential.kid(),
                jti, issuedAt, expiresAt);
    }

    private static Instant instant(Date value, WorkloadAssertionFailureReason reason) {
        if (value == null) throw failure(reason);
        try {
            return value.toInstant();
        } catch (RuntimeException exception) {
            throw failure(reason);
        }
    }

    private static void validateLifecycle(TrustedWorkloadCredential credential, Instant now) {
        if (credential.principalLifecycle() == WorkloadPrincipalLifecycle.DISABLED) throw failure(DISABLED_WORKLOAD);
        if (credential.principalLifecycle() == WorkloadPrincipalLifecycle.REVOKED) throw failure(REVOKED_WORKLOAD);

        switch (credential.credentialLifecycle()) {
            case PENDING, RETIRED -> throw failure(INACTIVE_CREDENTIAL);
            case REVOKED -> throw failure(REVOKED_CREDENTIAL);
            case ACTIVE -> { }
        }
        if (now.isBefore(credential.notBefore())) throw failure(CREDENTIAL_NOT_YET_VALID);
        if (credential.notAfter() != null && !now.isBefore(credential.notAfter())) {
            throw failure(CREDENTIAL_EXPIRED);
        }
    }

    private static WorkloadAssertionVerificationException failure(WorkloadAssertionFailureReason reason) {
        return new WorkloadAssertionVerificationException(reason);
    }
}
