package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import com.josecjuniors.logossrv.support.test.WorkloadTestKeys;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Clock;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@FreshPostgresIntegrationTest
class WorkloadAssertionVerifierPostgresTest {
    private static final String ISSUER = WorkloadAssertionProfile.ISSUER;
    @Autowired JdbcTemplate jdbc;
    @Autowired WorkloadAssertionVerifier verifier;
    @Autowired Clock clock;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM workload_signing_key WHERE workload_principal_id IN " +
                "(SELECT id FROM workload_principal WHERE issuer = ?)", ISSUER);
        jdbc.update("DELETE FROM workload_principal WHERE issuer = ?", ISSUER);
    }

    @Test
    void verifiesTheSameAssertionTwiceWithoutCreatingReplayState() throws Exception {
        KeyPair pair = WorkloadTestKeys.ec("secp256r1");
        Instant now = clock.instant();
        UUID principalId = UUID.randomUUID();
        UUID credentialId = UUID.randomUUID();
        Instant notBefore = now.minusSeconds(300);
        jdbc.update("""
                INSERT INTO workload_principal
                    (id, principal_type, principal_id, issuer, lifecycle_status)
                VALUES (?, 'WORKLOAD', 'lifeos', ?, 'ACTIVE')
                """, principalId, ISSUER);
        jdbc.update("""
                INSERT INTO workload_signing_key
                    (id, workload_principal_id, kid, algorithm, public_key_pem, public_key_spki_sha256,
                     lifecycle_status, not_before, not_after, activated_at)
                VALUES (?, ?, 'kid-postgres', 'ES256', ?, ?, 'ACTIVE', ?, NULL, ?)
                """, credentialId, principalId, WorkloadTestKeys.publicPem(pair),
                MessageDigest.getInstance("SHA-256").digest(pair.getPublic().getEncoded()),
                java.sql.Timestamp.from(notBefore), java.sql.Timestamp.from(notBefore));

        String assertion = Jwts.builder().header().type(WorkloadAssertionProfile.TYPE)
                .keyId("kid-postgres").and()
                .issuer(ISSUER).subject("lifeos").audience().add(WorkloadAssertionProfile.AUDIENCE).and()
                .issuedAt(Date.from(now.minusSeconds(1))).expiration(Date.from(now.plusSeconds(59)))
                .id(UUID.randomUUID().toString()).signWith(pair.getPrivate(), Jwts.SIG.ES256).compact();

        assertThat(verifier.verify(assertion).principalId().value()).isEqualTo("lifeos");
        assertThat(verifier.verify(assertion).principalId().value()).isEqualTo("lifeos");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_assertion_replay", Long.class)).isZero();
    }
}
