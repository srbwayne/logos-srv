package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.workload.application.port.out.WorkloadTrustRegistry;
import com.josecjuniors.logossrv.core.security.workload.domain.TrustedWorkloadCredential;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionFailureReason.*;

@Component
public class WorkloadAssertionVerifier {
    private final WorkloadAssertionPreParser preParser;
    private final WorkloadTrustRegistry trustRegistry;
    private final WorkloadAssertionClaimValidator claimValidator;
    private final Clock clock;

    public WorkloadAssertionVerifier(WorkloadAssertionPreParser preParser,
                                     WorkloadTrustRegistry trustRegistry,
                                     WorkloadAssertionClaimValidator claimValidator,
                                     Clock clock) {
        this.preParser = preParser;
        this.trustRegistry = trustRegistry;
        this.claimValidator = claimValidator;
        this.clock = clock;
    }

    public VerifiedWorkloadAssertion verify(String compactAssertion) {
        UntrustedWorkloadAssertionHints hints = preParser.parse(compactAssertion);
        Optional<TrustedWorkloadCredential> registered = trustRegistry.findByIssuerAndKid(
                hints.candidateIssuer(), hints.candidateKid());
        TrustedWorkloadCredential credential = registered.orElseThrow(
                () -> failure(UNKNOWN_CREDENTIAL));

        Instant now = clock.instant();
        Jws<Claims> verifiedJws = verifySignature(compactAssertion, credential, now);
        validateVerifiedHeader(verifiedJws, credential);
        return claimValidator.validate(verifiedJws.getPayload(), credential, now);
    }

    private static Jws<Claims> verifySignature(String compactAssertion, TrustedWorkloadCredential credential, Instant now) {
        try {
            var builder = Jwts.parser()
                    .verifyWith(credential.publicKey())
                    .clock(() -> Date.from(now))
                    .clockSkewSeconds(WorkloadAssertionProfile.CLOCK_SKEW.toSeconds());
            var signatures = builder.sig();
            signatures.remove(Jwts.SIG.NONE).remove(Jwts.SIG.HS256).remove(Jwts.SIG.HS384)
                    .remove(Jwts.SIG.HS512).remove(Jwts.SIG.RS256).remove(Jwts.SIG.RS384)
                    .remove(Jwts.SIG.RS512).remove(Jwts.SIG.PS256).remove(Jwts.SIG.PS384)
                    .remove(Jwts.SIG.PS512).remove(Jwts.SIG.ES384).remove(Jwts.SIG.ES512)
                    .remove(Jwts.SIG.EdDSA);
            return signatures.and().build().parseSignedClaims(compactAssertion);
        } catch (ExpiredJwtException exception) {
            throw failure(EXP_INVALID);
        } catch (SignatureException exception) {
            throw new WorkloadAssertionVerificationException(SIGNATURE_INVALID);
        } catch (MalformedJwtException exception) {
            throw new WorkloadAssertionVerificationException(MALFORMED_ASSERTION);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new WorkloadAssertionVerificationException(SIGNATURE_INVALID);
        }
    }

    private static void validateVerifiedHeader(Jws<Claims> verifiedJws, TrustedWorkloadCredential credential) {
        var header = verifiedJws.getHeader();
        if (!header.keySet().equals(Set.of("alg", "typ", "kid"))) throw failure(MALFORMED_ASSERTION);
        if (!WorkloadAssertionProfile.ALGORITHM.equals(header.getAlgorithm())) throw failure(WRONG_ALGORITHM);
        if (!WorkloadAssertionProfile.TYPE.equals(header.getType())) throw failure(WRONG_TYPE);
        if (!credential.kid().value().equals(header.getKeyId())) throw failure(INVALID_KID);
        if (!header.isPayloadEncoded()) throw failure(MALFORMED_ASSERTION);
    }

    private static WorkloadAssertionVerificationException failure(WorkloadAssertionFailureReason reason) {
        return new WorkloadAssertionVerificationException(reason);
    }
}
