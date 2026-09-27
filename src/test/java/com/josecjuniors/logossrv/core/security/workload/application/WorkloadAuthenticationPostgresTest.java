package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationMethod;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationStatus;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadAssertionReplayException;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionVerificationException;
import com.josecjuniors.logossrv.core.security.workload.application.port.out.WorkloadAssertionReplayStore;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import com.josecjuniors.logossrv.support.test.WorkloadTestKeys;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.ISSUER;
import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.AUDIENCE;
import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FreshPostgresIntegrationTest
@Import(WorkloadAuthenticationPostgresTest.RollbackTestConfiguration.class)
class WorkloadAuthenticationPostgresTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WorkloadAuthenticationService authenticationService;
    @Autowired WorkloadAssertionReplayStore replayStore;
    @Autowired RollbackAuthentication rollbackAuthentication;
    @Autowired Clock clock;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM workload_assertion_replay WHERE issuer = ?", ISSUER);
        jdbc.update("DELETE FROM workload_trust_audit_event WHERE workload_principal_id IN " +
                "(SELECT id FROM workload_principal WHERE issuer = ?)", ISSUER);
        jdbc.update("DELETE FROM workload_signing_key WHERE workload_principal_id IN " +
                "(SELECT id FROM workload_principal WHERE issuer = ?)", ISSUER);
        jdbc.update("DELETE FROM workload_principal WHERE issuer = ?", ISSUER);
    }

    @Test
    void firstUseReturnsOnlyNormalizedPrincipalAndSecondUseIsRejected() throws Exception {
        Fixture fixture = fixture("kid-first");
        String token = assertion(fixture, UUID.randomUUID());

        var principal = authenticationService.authenticate(token);
        assertThat(principal.principalType()).isEqualTo(PrincipalType.WORKLOAD);
        assertThat(principal.principalId()).isEqualTo("lifeos");
        assertThat(principal.authenticationMethod()).isEqualTo(AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION);
        assertThat(principal.credentialIdentity()).isEqualTo("kid-first");
        assertThat(principal.authenticationStatus()).isEqualTo(AuthenticationStatus.VERIFIED);
        assertThat(replayCount()).isEqualTo(1);
        assertReplayMatches(token, fixture);

        assertThatThrownBy(() -> authenticationService.authenticate(token))
                .isInstanceOf(WorkloadAssertionReplayException.class);
        assertThat(replayCount()).isEqualTo(1);
    }

    @Test
    void distinctJtiValuesAreIndependentReplayIdentities() throws Exception {
        Fixture fixture = fixture("kid-different-jti");
        authenticationService.authenticate(assertion(fixture, UUID.randomUUID()));
        authenticationService.authenticate(assertion(fixture, UUID.randomUUID()));
        assertThat(replayCount()).isEqualTo(2);
    }

    @Test
    void simultaneousIdenticalAssertionsHaveExactlyOneSuccessfulAuthentication() throws Exception {
        Fixture fixture = fixture("kid-concurrent");
        String token = assertion(fixture, UUID.randomUUID());
        int workers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger replays = new AtomicInteger();
        try {
            @SuppressWarnings("unchecked") Future<Void>[] futures = new Future[workers];
            for (int i = 0; i < workers; i++) {
                futures[i] = executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timed out");
                    try {
                        authenticationService.authenticate(token);
                        successes.incrementAndGet();
                    } catch (WorkloadAssertionReplayException expected) {
                        replays.incrementAndGet();
                    }
                    return null;
                });
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Void> future : futures) future.get(45, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertThat(successes).hasValue(1);
        assertThat(replays).hasValue(workers - 1);
        assertThat(replayCount()).isEqualTo(1);
    }

    @Test
    void replayCommitSurvivesOuterTransactionRollback() throws Exception {
        Fixture fixture = fixture("kid-rollback");
        String token = assertion(fixture, UUID.randomUUID());

        assertThatThrownBy(() -> rollbackAuthentication.authenticateThenFail(token))
                .isInstanceOf(RollbackAuthentication.DeliberateRollback.class);

        assertThat(replayCount()).isEqualTo(1);
        assertThatThrownBy(() -> authenticationService.authenticate(token))
                .isInstanceOf(WorkloadAssertionReplayException.class);
    }

    @Test
    void cleanupUsesStrictSkewCutoffAndDeletesOnlyBoundedBatches() {
        Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var cleanup = new WorkloadAssertionReplayCleanup(replayStore, Clock.fixed(now, java.time.ZoneOffset.UTC));
        UUID principalId = createPrincipal();
        insertReplay(now.minusSeconds(16), 5);
        insertReplay(now.minusSeconds(15), 1);
        insertReplay(now.minusSeconds(14), 1);
        insertReplay(now.plusSeconds(60), 1);

        assertThat(cleanup.cleanupExpired(3)).isEqualTo(3);
        assertThat(replayCount()).isEqualTo(5);
        assertThat(cleanup.cleanupExpired(3)).isEqualTo(2);
        assertThat(replayCount()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_assertion_replay WHERE issuer = ? " +
                "AND expires_at >= ?", Long.class, ISSUER, java.sql.Timestamp.from(now.minusSeconds(15))))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_principal WHERE id = ?", Long.class, principalId))
                .isEqualTo(1);
    }

    @Test
    void invalidAssertionDoesNotConsumeReplay() throws Exception {
        Fixture fixture = fixture("kid-invalid");
        String valid = assertion(fixture, UUID.randomUUID());
        String[] segments = valid.split("\\.");
        segments[2] = (segments[2].charAt(0) == 'A' ? "B" : "A") + segments[2].substring(1);
        String invalid = String.join(".", segments);
        assertThatThrownBy(() -> authenticationService.authenticate(invalid))
                .isInstanceOf(WorkloadAssertionVerificationException.class);
        assertThat(replayCount()).isZero();
    }

    private Fixture fixture(String kid) throws Exception {
        KeyPair pair = WorkloadTestKeys.ec("secp256r1");
        UUID principalDbId = createPrincipal();
        UUID credentialDbId = UUID.randomUUID();
        Instant notBefore = clock.instant().minusSeconds(300);
        jdbc.update("""
                INSERT INTO workload_signing_key
                    (id, workload_principal_id, kid, algorithm, public_key_pem, public_key_spki_sha256,
                     lifecycle_status, not_before, not_after, activated_at)
                VALUES (?, ?, ?, 'ES256', ?, ?, 'ACTIVE', ?, NULL, ?)
                """, credentialDbId, principalDbId, kid, WorkloadTestKeys.publicPem(pair),
                MessageDigest.getInstance("SHA-256").digest(pair.getPublic().getEncoded()),
                java.sql.Timestamp.from(notBefore), java.sql.Timestamp.from(notBefore));
        return new Fixture(pair, kid);
    }

    private UUID createPrincipal() {
        UUID principalDbId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO workload_principal
                    (id, principal_type, principal_id, issuer, lifecycle_status)
                VALUES (?, 'WORKLOAD', 'lifeos', ?, 'ACTIVE')
                """, principalDbId, ISSUER);
        return principalDbId;
    }

    private String assertion(Fixture fixture, UUID jti) {
        Instant now = clock.instant();
        return Jwts.builder().header().type(TYPE).keyId(fixture.kid()).and()
                .issuer(ISSUER).subject("lifeos").audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now.minusSeconds(1))).expiration(Date.from(now.plusSeconds(59)))
                .id(jti.toString()).signWith(fixture.keyPair().getPrivate(), Jwts.SIG.ES256).compact();
    }

    private void assertReplayMatches(String token, Fixture ignored) {
        // The signed JWT expiration is represented at millisecond precision by JJWT and stored unchanged.
        var parsed = Jwts.parser().verifyWith(ignored.keyPair().getPublic()).build().parseSignedClaims(token);
        UUID jti = UUID.fromString(parsed.getPayload().getId());
        Instant exp = parsed.getPayload().getExpiration().toInstant();
        assertThat(jdbc.queryForObject("SELECT expires_at FROM workload_assertion_replay WHERE issuer = ? AND jti = ?",
                java.sql.Timestamp.class, ISSUER, jti).toInstant()).isEqualTo(exp);
        assertThat(jdbc.queryForObject("SELECT consumed_at FROM workload_assertion_replay WHERE issuer = ? AND jti = ?",
                java.sql.Timestamp.class, ISSUER, jti)).isNotNull();
    }

    private long replayCount() {
        return jdbc.queryForObject("SELECT count(*) FROM workload_assertion_replay WHERE issuer = ?", Long.class, ISSUER);
    }

    private void insertReplay(Instant expiresAt, int count) {
        for (int i = 0; i < count; i++) {
            replayStore.consume(new com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer(ISSUER),
                    UUID.randomUUID(), expiresAt);
        }
    }

    private record Fixture(KeyPair keyPair, String kid) { }

    interface RollbackAuthentication {
        void authenticateThenFail(String assertion);

        class DeliberateRollback extends RuntimeException { }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RollbackTestConfiguration {
        @Bean
        RollbackAuthentication rollbackAuthentication(WorkloadAuthenticationService service) {
            return new RollbackAuthenticationBean(service);
        }
    }

    static class RollbackAuthenticationBean implements RollbackAuthentication {
        private final WorkloadAuthenticationService service;

        RollbackAuthenticationBean(WorkloadAuthenticationService service) {
            this.service = service;
        }

        @Override
        @org.springframework.transaction.annotation.Transactional
        public void authenticateThenFail(String assertion) {
            service.authenticate(assertion);
            throw new DeliberateRollback();
        }
    }
}
