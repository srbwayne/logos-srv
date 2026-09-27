package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadTrustRegistryIntegrityException;
import com.josecjuniors.logossrv.core.security.workload.application.port.out.WorkloadTrustRegistry;
import com.josecjuniors.logossrv.core.security.workload.domain.*;
import com.josecjuniors.logossrv.support.test.WorkloadTestKeys;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionFailureReason.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkloadAssertionVerificationTest {
    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");
    private static KeyPair pair;

    @BeforeAll
    static void keys() throws Exception { pair = WorkloadTestKeys.ec("secp256r1"); }

    @Test
    void verifiesCanonicalAssertionAndDoesNotConsumeReplayOnRepeatedCalls() {
        var verifier = verifier(WorkloadPrincipalLifecycle.ACTIVE, WorkloadCredentialLifecycle.ACTIVE,
                NOW.minusSeconds(60), NOW.plusSeconds(600), null);
        String token = token(pair, Map.of(), Map.of());

        var first = verifier.verify(token);
        var second = verifier.verify(token);

        assertThat(first).isEqualTo(second);
        assertThat(first.issuer().value()).isEqualTo(WorkloadAssertionProfile.ISSUER);
        assertThat(first.principalId().value()).isEqualTo(WorkloadAssertionProfile.SUBJECT);
        assertThat(first.kid().value()).isEqualTo("kid-test");
        assertThat(first.jti()).isNotNull();
        assertThat(first).isInstanceOf(VerifiedWorkloadAssertion.class);
        assertThat(Arrays.stream(VerifiedWorkloadAssertion.class.getRecordComponents()).map(c -> c.getName()))
                .containsExactly("issuer", "principalId", "kid", "jti", "issuedAt", "expiresAt");
    }

    @Test
    void lookupUsesOnlyUntrustedIssuerAndKidAndSignedSubjectIsBoundToCredential() {
        AtomicReference<String> lookedUpSubject = new AtomicReference<>();
        WorkloadTrustRegistry registry = (issuer, kid) -> {
            lookedUpSubject.set("not-an-input");
            assertThat(issuer.value()).isEqualTo(WorkloadAssertionProfile.ISSUER);
            assertThat(kid.value()).isEqualTo("kid-test");
            return Optional.of(credential(WorkloadPrincipalLifecycle.ACTIVE,
                    WorkloadCredentialLifecycle.ACTIVE, NOW.minusSeconds(60), NOW.plusSeconds(600), null));
        };
        var verifier = verifier(registry);
        assertThatThrownBy(() -> verifier.verify(token(pair, Map.of(), Map.of("sub", "victim"))))
                .isInstanceOf(WorkloadAssertionVerificationException.class)
                .extracting(e -> ((WorkloadAssertionVerificationException) e).reason()).isEqualTo(SUBJECT_INVALID);
        assertThat(lookedUpSubject).hasValue("not-an-input");
    }

    @Test
    void rejectsWrongSignatureAndTampering() throws Exception {
        KeyPair other = WorkloadTestKeys.ec("secp256r1");
        var verifier = verifier(WorkloadPrincipalLifecycle.ACTIVE, WorkloadCredentialLifecycle.ACTIVE,
                NOW.minusSeconds(60), NOW.plusSeconds(600), null);
        assertReason(verifier, token(other, Map.of(), Map.of()), SIGNATURE_INVALID);
        String valid = token(pair, Map.of(), Map.of());
        String[] parts = valid.split("\\.");
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"iss\":\"urn:akume:workload-issuer:lifeos\",\"sub\":\"lifeos\",\"aud\":\"urn:akume:service:logos\",\"iat\":1788264000,\"exp\":1788264060,\"jti\":\"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa\"}".getBytes()) + "." + parts[2];
        assertReason(verifier, tampered, SIGNATURE_INVALID);
        byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
        signature[0] ^= 0x01;
        assertReason(verifier, parts[0] + "." + parts[1] + "." +
                Base64.getUrlEncoder().withoutPadding().encodeToString(signature), SIGNATURE_INVALID);
        assertReason(verifier, tokenWithKid(pair, "other-kid", Map.of()), INVALID_KID);
    }

    @Test
    void enforcesAudienceIssuerAndJtiSemantics() {
        var verifier = verifier(WorkloadPrincipalLifecycle.ACTIVE, WorkloadCredentialLifecycle.ACTIVE,
                NOW.minusSeconds(60), NOW.plusSeconds(600), null);
        assertReason(verifier, token(pair, Map.of("aud", List.of(WorkloadAssertionProfile.AUDIENCE, "other")), Map.of()), AUDIENCE_INVALID);
        assertReason(verifier, token(pair, Map.of("iss", "urn:wrong"), Map.of()), ISSUER_INVALID);
        assertReason(verifier, token(pair, Map.of(), Map.of("jti", "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA")), JTI_INVALID);
        assertReason(verifier, token(pair, Map.of(), Map.of("jti", "aaaaaaaa-aaaa-1aaa-8aaa-aaaaaaaaaaaa")), JTI_INVALID);
        assertReason(verifier, token(pair, Map.of(), Map.of("exp", NOW.minusSeconds(1))), EXP_INVALID);
        assertReason(verifier, token(pair, Map.of(), Map.of("iat", NOW.plusSeconds(20))), IAT_INVALID);
    }

    @Test
    void enforcesTtlAndClockBoundaries() {
        var verifier = verifier(WorkloadPrincipalLifecycle.ACTIVE, WorkloadCredentialLifecycle.ACTIVE,
                NOW.minusSeconds(600), NOW.plusSeconds(600), null);
        assertThat(verifier.verify(tokenAt(pair, NOW, NOW.plusSeconds(60), Map.of()))).isNotNull();
        assertReason(verifier, tokenAt(pair, NOW, NOW.plusSeconds(61), Map.of()), EXP_INVALID);
        assertThat(verifier.verify(tokenAt(pair, NOW.plusSeconds(15), NOW.plusSeconds(75), Map.of()))).isNotNull();
        assertReason(verifier, tokenAt(pair, NOW.plusSeconds(16), NOW.plusSeconds(76), Map.of()), IAT_INVALID);
        assertThat(verifier.verify(tokenAt(pair, NOW.minusSeconds(74), NOW.minusSeconds(14), Map.of()))).isNotNull();
        assertReason(verifier, tokenAt(pair, NOW.minusSeconds(76), NOW.minusSeconds(16), Map.of()), EXP_INVALID);
    }

    @Test
    void enforcesPrincipalCredentialAndKeyValidityLifecycle() {
        assertReason(verifier(WorkloadPrincipalLifecycle.DISABLED, WorkloadCredentialLifecycle.ACTIVE,
                NOW.minusSeconds(10), null, null), token(pair, Map.of(), Map.of()), DISABLED_WORKLOAD);
        assertReason(verifier(WorkloadPrincipalLifecycle.REVOKED, WorkloadCredentialLifecycle.ACTIVE,
                NOW.minusSeconds(10), null, null), token(pair, Map.of(), Map.of()), REVOKED_WORKLOAD);
        for (var state : List.of(WorkloadCredentialLifecycle.PENDING, WorkloadCredentialLifecycle.REVOKED,
                WorkloadCredentialLifecycle.RETIRED)) {
            var expected = state == WorkloadCredentialLifecycle.REVOKED ? REVOKED_CREDENTIAL : INACTIVE_CREDENTIAL;
            assertReason(verifier(WorkloadPrincipalLifecycle.ACTIVE, state, NOW.minusSeconds(10), null, null),
                    token(pair, Map.of(), Map.of()), expected);
        }
        assertReason(verifier(WorkloadPrincipalLifecycle.ACTIVE, WorkloadCredentialLifecycle.ACTIVE,
                NOW.plusSeconds(1), null, null), token(pair, Map.of(), Map.of()), CREDENTIAL_NOT_YET_VALID);
        assertThat(verifier(WorkloadPrincipalLifecycle.ACTIVE, WorkloadCredentialLifecycle.ACTIVE,
                NOW, null, null).verify(token(pair, Map.of(), Map.of()))).isNotNull();
        assertReason(verifier(WorkloadPrincipalLifecycle.ACTIVE, WorkloadCredentialLifecycle.ACTIVE,
                NOW.minusSeconds(1), NOW, null), token(pair, Map.of(), Map.of()), CREDENTIAL_EXPIRED);
    }

    @Test
    void unknownCredentialAndRegistryCorruptionRemainDistinct() {
        var unknown = verifier((issuer, kid) -> Optional.empty());
        assertReason(unknown, token(pair, Map.of(), Map.of()), UNKNOWN_CREDENTIAL);
        var corrupt = verifier((issuer, kid) -> { throw new WorkloadTrustRegistryIntegrityException("trust registry integrity failure"); });
        assertThatThrownBy(() -> corrupt.verify(token(pair, Map.of(), Map.of())))
                .isInstanceOf(WorkloadTrustRegistryIntegrityException.class);
    }

    private static WorkloadAssertionVerifier verifier(WorkloadPrincipalLifecycle principal,
            WorkloadCredentialLifecycle lifecycle, Instant notBefore, Instant notAfter, Instant unused) {
        return verifier((issuer, kid) -> Optional.of(credential(principal, lifecycle, notBefore, notAfter, unused)));
    }
    private static WorkloadAssertionVerifier verifier(WorkloadTrustRegistry registry) {
        return new WorkloadAssertionVerifier(new WorkloadAssertionPreParser(), registry,
                new WorkloadAssertionClaimValidator(), Clock.fixed(NOW, ZoneOffset.UTC));
    }
    private static TrustedWorkloadCredential credential(WorkloadPrincipalLifecycle principal,
            WorkloadCredentialLifecycle lifecycle, Instant notBefore, Instant notAfter, Instant unused) {
        return new TrustedWorkloadCredential(UUID.randomUUID(), UUID.randomUUID(),
                new WorkloadIssuer(WorkloadAssertionProfile.ISSUER), new WorkloadPrincipalId(WorkloadAssertionProfile.SUBJECT),
                principal, null, null, new WorkloadKeyId("kid-test"), WorkloadSignatureAlgorithm.ES256,
                (java.security.interfaces.ECPublicKey) pair.getPublic(), lifecycle, notBefore, notAfter, NOW, null, null);
    }
    private static String token(KeyPair signing, Map<String, Object> claimOverrides, Map<String, Object> otherClaims) {
        Map<String, Object> overrides = merge(claimOverrides, otherClaims);
        return tokenAt(signing, (Instant) overrides.getOrDefault("iat", NOW.minusSeconds(1)),
                (Instant) overrides.getOrDefault("exp", NOW.plusSeconds(59)), overrides);
    }
    private static String tokenAt(KeyPair signing, Instant issued, Instant expires, Map<String, Object> overrides) {
        return tokenWithKid(signing, "kid-test", overrides, issued, expires);
    }
    private static String tokenWithKid(KeyPair signing, String kid, Map<String, Object> overrides) {
        return tokenWithKid(signing, kid, overrides, NOW.minusSeconds(1), NOW.plusSeconds(59));
    }
    private static String tokenWithKid(KeyPair signing, String kid, Map<String, Object> overrides,
                                       Instant issued, Instant expires) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", WorkloadAssertionProfile.ISSUER);
        claims.put("sub", WorkloadAssertionProfile.SUBJECT);
        claims.put("aud", WorkloadAssertionProfile.AUDIENCE);
        claims.put("iat", Date.from(issued));
        claims.put("exp", Date.from(expires));
        claims.put("jti", UUID.randomUUID().toString());
        overrides.forEach((key, value) -> claims.put(key, value instanceof Instant instant ? Date.from(instant) : value));
        return Jwts.builder().header().type(WorkloadAssertionProfile.TYPE).keyId(kid).and()
                .claims(claims).signWith(signing.getPrivate(), Jwts.SIG.ES256).compact();
    }
    private static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> result = new HashMap<>(a); result.putAll(b); return result;
    }
    private static void assertReason(WorkloadAssertionVerifier verifier, String token,
                                     WorkloadAssertionFailureReason reason) {
        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(WorkloadAssertionVerificationException.class)
                .extracting(e -> ((WorkloadAssertionVerificationException) e).reason()).isEqualTo(reason);
    }
}
