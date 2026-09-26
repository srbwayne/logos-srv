package com.josecjuniors.logossrv.adapters.out.security.workload.jpa;

import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadTrustRegistryIntegrityException;
import com.josecjuniors.logossrv.core.security.workload.application.port.out.WorkloadTrustRegistry;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadCredentialLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadSignatureAlgorithm;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import com.josecjuniors.logossrv.support.test.WorkloadTestKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FreshPostgresIntegrationTest
class JpaWorkloadTrustRegistryPostgresTest {
    private static final Instant NOT_BEFORE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired WorkloadTrustRegistry registry;

    @BeforeEach
    void cleanTrustFixtures() {
        clean();
    }

    @AfterEach
    void leaveNoTrustFixtures() {
        clean();
    }

    @Test
    void resolvesActiveCredentialAndReturnsCoherentPublicTrustRecordWithoutSideEffects() throws Exception {
        KeyPair pair = WorkloadTestKeys.ec("secp256r1");
        UUID principalId = insertPrincipal("resolved-lifeos", "urn:akume:test:issuer-resolved", "ACTIVE");
        UUID credentialId = insertCredential(principalId, "kid-active", pair, "ACTIVE");
        String principalsBefore = snapshot("workload_principal", "id");
        String credentialsBefore = snapshot("workload_signing_key", "id");
        String auditsBefore = snapshot("workload_trust_audit_event", "id");
        String replaysBefore = snapshot("workload_assertion_replay", "issuer, jti");

        var found = registry.findByIssuerAndKid(new WorkloadIssuer("urn:akume:test:issuer-resolved"),
                new WorkloadKeyId("kid-active")).orElseThrow();

        assertThat(found.principalDatabaseId()).isEqualTo(principalId);
        assertThat(found.credentialDatabaseId()).isEqualTo(credentialId);
        assertThat(found.issuer().value()).isEqualTo("urn:akume:test:issuer-resolved");
        assertThat(found.principalId()).isEqualTo(new WorkloadPrincipalId("resolved-lifeos"));
        assertThat(found.principalLifecycle()).isEqualTo(WorkloadPrincipalLifecycle.ACTIVE);
        assertThat(found.kid()).isEqualTo(new WorkloadKeyId("kid-active"));
        assertThat(found.algorithm()).isEqualTo(WorkloadSignatureAlgorithm.ES256);
        assertThat(found.publicKey()).isInstanceOf(java.security.interfaces.ECPublicKey.class);
        assertThat(found.publicKey().getParams().getOrder().bitLength()).isEqualTo(256);
        assertThat(found.credentialLifecycle()).isEqualTo(WorkloadCredentialLifecycle.ACTIVE);
        assertThat(found.notBefore()).isEqualTo(NOT_BEFORE);
        assertThat(found.notAfter()).isEqualTo(NOT_BEFORE.plusSeconds(600));
        assertThat(found.activatedAt()).isEqualTo(NOT_BEFORE);
        assertThat(found.revokedAt()).isNull();
        assertThat(found.retiredAt()).isNull();

        assertThat(snapshot("workload_principal", "id")).isEqualTo(principalsBefore);
        assertThat(snapshot("workload_signing_key", "id")).isEqualTo(credentialsBefore);
        assertThat(snapshot("workload_trust_audit_event", "id")).isEqualTo(auditsBefore);
        assertThat(snapshot("workload_assertion_replay", "issuer, jti")).isEqualTo(replaysBefore);
    }

    @Test
    void lookupIsScopedByExactIssuerAndReturnsEmptyForUnknownIssuerOrKid() throws Exception {
        UUID principalA = insertPrincipal("scope-a", "urn:akume:test:issuer-a", "ACTIVE");
        UUID principalB = insertPrincipal("scope-b", "urn:akume:test:issuer-b", "ACTIVE");
        insertCredential(principalA, "kid-shared", WorkloadTestKeys.ec("secp256r1"), "ACTIVE");
        insertCredential(principalB, "kid-shared", WorkloadTestKeys.ec("secp256r1"), "ACTIVE");

        assertThat(registry.findByIssuerAndKid(new WorkloadIssuer("urn:akume:test:issuer-a"),
                new WorkloadKeyId("kid-shared")).orElseThrow().principalId().value()).isEqualTo("scope-a");
        assertThat(registry.findByIssuerAndKid(new WorkloadIssuer("urn:akume:test:issuer-b"),
                new WorkloadKeyId("kid-shared")).orElseThrow().principalId().value()).isEqualTo("scope-b");
        assertThat(registry.findByIssuerAndKid(new WorkloadIssuer("urn:akume:test:unknown"),
                new WorkloadKeyId("kid-shared"))).isEmpty();
        assertThat(registry.findByIssuerAndKid(new WorkloadIssuer("urn:akume:test:issuer-a"),
                new WorkloadKeyId("unknown-kid"))).isEmpty();
    }

    @Test
    void returnsPrincipalAndCredentialLifecycleRowsWithoutFilteringThem() throws Exception {
        for (String status : new String[]{"ACTIVE", "DISABLED", "REVOKED"}) {
            UUID principal = insertPrincipal("principal-" + status.toLowerCase(),
                    "urn:akume:test:principal-" + status.toLowerCase(), status);
            insertCredential(principal, "kid-active-" + status.toLowerCase(),
                    WorkloadTestKeys.ec("secp256r1"), "ACTIVE");
        }
        String[] keyStates = {"PENDING", "REVOKED", "RETIRED"};
        for (String status : keyStates) {
            UUID principal = insertPrincipal("credential-" + status.toLowerCase(),
                    "urn:akume:test:credential-" + status.toLowerCase(), "ACTIVE");
            insertCredential(principal, "kid-" + status.toLowerCase(),
                    WorkloadTestKeys.ec("secp256r1"), status);
        }

        var disabledPrincipal = resolve("urn:akume:test:principal-disabled", "kid-active-disabled").orElseThrow();
        assertThat(disabledPrincipal.principalLifecycle()).isEqualTo(WorkloadPrincipalLifecycle.DISABLED);
        assertThat(disabledPrincipal.principalDisabledAt()).isEqualTo(NOT_BEFORE);
        var revokedPrincipal = resolve("urn:akume:test:principal-revoked", "kid-active-revoked").orElseThrow();
        assertThat(revokedPrincipal.principalLifecycle()).isEqualTo(WorkloadPrincipalLifecycle.REVOKED);
        assertThat(revokedPrincipal.principalDisabledAt()).isEqualTo(NOT_BEFORE);
        assertThat(revokedPrincipal.principalRevokedAt()).isEqualTo(NOT_BEFORE.plusSeconds(30));
        var pending = resolve("urn:akume:test:credential-pending", "kid-pending").orElseThrow();
        assertThat(pending.credentialLifecycle()).isEqualTo(WorkloadCredentialLifecycle.PENDING);
        assertThat(pending.activatedAt()).isNull();
        var revoked = resolve("urn:akume:test:credential-revoked", "kid-revoked").orElseThrow();
        assertThat(revoked.credentialLifecycle()).isEqualTo(WorkloadCredentialLifecycle.REVOKED);
        assertThat(revoked.revokedAt()).isEqualTo(NOT_BEFORE.plusSeconds(20));
        var retired = resolve("urn:akume:test:credential-retired", "kid-retired").orElseThrow();
        assertThat(retired.credentialLifecycle()).isEqualTo(WorkloadCredentialLifecycle.RETIRED);
        assertThat(retired.revokedAt()).isEqualTo(NOT_BEFORE.plusSeconds(20));
        assertThat(retired.retiredAt()).isEqualTo(NOT_BEFORE.plusSeconds(30));
    }

    @Test
    void rejectsFingerprintMismatchRatherThanReturningNotFound() throws Exception {
        UUID principal = insertPrincipal("fingerprint-mismatch", "urn:akume:test:fingerprint-mismatch", "ACTIVE");
        insertCredential(principal, "kid-fingerprint", WorkloadTestKeys.ec("secp256r1"), "ACTIVE", filled(77));

        assertIntegrityFailure("urn:akume:test:fingerprint-mismatch", "kid-fingerprint");
    }

    @Test
    void rejectsWrongCurveWrongKeyTypeAndMalformedPersistedPem() throws Exception {
        UUID p384Principal = insertPrincipal("wrong-curve", "urn:akume:test:wrong-curve", "ACTIVE");
        insertCredential(p384Principal, "kid-p384", WorkloadTestKeys.ec("secp384r1"), "ACTIVE");

        UUID rsaPrincipal = insertPrincipal("wrong-key-type", "urn:akume:test:wrong-key-type", "ACTIVE");
        insertCredential(rsaPrincipal, "kid-rsa", WorkloadTestKeys.rsa(), "ACTIVE");

        UUID malformedPrincipal = insertPrincipal("malformed-pem", "urn:akume:test:malformed-pem", "ACTIVE");
        insertMalformedCredential(malformedPrincipal, "kid-malformed");

        assertIntegrityFailure("urn:akume:test:wrong-curve", "kid-p384");
        assertIntegrityFailure("urn:akume:test:wrong-key-type", "kid-rsa");
        assertIntegrityFailure("urn:akume:test:malformed-pem", "kid-malformed");
    }

    private Optional<com.josecjuniors.logossrv.core.security.workload.domain.TrustedWorkloadCredential> resolve(
            String issuer, String kid) {
        return registry.findByIssuerAndKid(new WorkloadIssuer(issuer), new WorkloadKeyId(kid));
    }

    private void assertIntegrityFailure(String issuer, String kid) {
        assertThatThrownBy(() -> resolve(issuer, kid))
                .isInstanceOf(WorkloadTrustRegistryIntegrityException.class)
                .hasMessageContaining("integrity failure")
                .hasMessageNotContaining("BEGIN PUBLIC KEY");
    }

    private UUID insertPrincipal(String principalId, String issuer, String lifecycle) {
        UUID id = UUID.randomUUID();
        Instant disabled = "DISABLED".equals(lifecycle) || "REVOKED".equals(lifecycle) ? NOT_BEFORE : null;
        Instant revoked = "REVOKED".equals(lifecycle) ? NOT_BEFORE.plusSeconds(30) : null;
        jdbc.update("""
                INSERT INTO workload_principal
                    (id, principal_type, principal_id, issuer, lifecycle_status, disabled_at, revoked_at)
                VALUES (?, 'WORKLOAD', ?, ?, ?, ?, ?)
                """, id, principalId, issuer, lifecycle, timestamp(disabled), timestamp(revoked));
        return id;
    }

    private UUID insertCredential(UUID principalId, String kid, KeyPair pair, String lifecycle) throws Exception {
        return insertCredential(principalId, kid, pair, lifecycle, MessageDigest.getInstance("SHA-256")
                .digest(pair.getPublic().getEncoded()));
    }

    private UUID insertCredential(UUID principalId, String kid, KeyPair pair, String lifecycle,
                                  byte[] fingerprint) {
        UUID id = UUID.randomUUID();
        Instant activated = "ACTIVE".equals(lifecycle) || "RETIRED".equals(lifecycle) ? NOT_BEFORE : null;
        Instant revoked = "REVOKED".equals(lifecycle) || "RETIRED".equals(lifecycle)
                ? NOT_BEFORE.plusSeconds(20) : null;
        Instant retired = "RETIRED".equals(lifecycle) ? NOT_BEFORE.plusSeconds(30) : null;
        jdbc.update("""
                INSERT INTO workload_signing_key
                    (id, workload_principal_id, kid, algorithm, public_key_pem, public_key_spki_sha256,
                     lifecycle_status, not_before, not_after, activated_at, revoked_at, retired_at)
                VALUES (?, ?, ?, 'ES256', ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, principalId, kid, WorkloadTestKeys.publicPem(pair), fingerprint, lifecycle,
                timestamp(NOT_BEFORE), timestamp(NOT_BEFORE.plusSeconds(600)), timestamp(activated),
                timestamp(revoked), timestamp(retired));
        return id;
    }

    private void insertMalformedCredential(UUID principalId, String kid) {
        jdbc.update("""
                INSERT INTO workload_signing_key
                    (workload_principal_id, kid, algorithm, public_key_pem, public_key_spki_sha256,
                     lifecycle_status, not_before, activated_at)
                VALUES (?, ?, 'ES256', 'not a PEM key', ?, 'ACTIVE', ?, ?)
                """, principalId, kid, filled(19), timestamp(NOT_BEFORE), timestamp(NOT_BEFORE));
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static byte[] filled(int value) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    private String snapshot(String table, String orderBy) {
        return jdbc.queryForObject("SELECT COALESCE(jsonb_agg(to_jsonb(t) ORDER BY " + orderBy
                + "), '[]'::jsonb)::text FROM " + table + " t", String.class);
    }

    private void clean() {
        jdbc.update("DELETE FROM workload_assertion_replay");
        jdbc.update("DELETE FROM workload_trust_audit_event");
        jdbc.update("DELETE FROM workload_signing_key");
        jdbc.update("DELETE FROM workload_principal");
    }
}
