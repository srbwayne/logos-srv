package com.josecjuniors.logossrv.adapters.out.security.authorization;

import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.authorization.application.port.out.AuthorizationGrantStore.InsertResult;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationGrant;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationPrincipal;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationSource;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import com.josecjuniors.logossrv.support.test.WorkloadTestKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@FreshPostgresIntegrationTest
class JdbcAuthorizationGrantStorePostgresTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired JdbcAuthorizationGrantStore store;

    private final String principalId = "lifeos";
    private final UUID principalDbId = UUID.randomUUID();
    private final String issuer = "urn:logos:test:authorization-store:" + principalDbId;
    private final String otherPrincipalId = "authorization-test-" + UUID.randomUUID();
    private final UUID otherPrincipalDbId = UUID.randomUUID();
    private final String otherIssuer = "urn:logos:test:authorization-store:" + otherPrincipalDbId;

    @BeforeEach
    void createPrincipals() {
        createPrincipal(principalDbId, principalId, issuer);
        createPrincipal(otherPrincipalDbId, otherPrincipalId, otherIssuer);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM authorization_grant WHERE principal_type='WORKLOAD' AND principal_id IN (?, ?)",
                principalId, otherPrincipalId);
        jdbc.update("DELETE FROM workload_signing_key WHERE workload_principal_id IN (?, ?)",
                principalDbId, otherPrincipalDbId);
        jdbc.update("DELETE FROM workload_trust_audit_event WHERE workload_principal_id IN (?, ?)",
                principalDbId, otherPrincipalDbId);
        jdbc.update("DELETE FROM workload_principal WHERE id IN (?, ?)", principalDbId, otherPrincipalDbId);
    }

    @Test
    void insertsReadsAndRetriesExactSemanticGrantWithoutChangingCreationTime() {
        AuthorizationGrant grant = execute(principalId, "lifeos", "lifeos");
        assertThat(store.insertIfAbsent(grant)).isEqualTo(InsertResult.CREATED);
        Timestamp createdAt = jdbc.queryForObject("SELECT created_at FROM authorization_grant "
                + "WHERE principal_id=?", Timestamp.class, principalId);
        assertThat(store.findExact(grant)).contains(grant);
        assertThat(store.findExact(execute(principalId, "lifeos", "other"))).isEmpty();
        assertThat(store.insertIfAbsent(grant)).isEqualTo(InsertResult.ALREADY_EXISTS);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM authorization_grant "
                + "WHERE principal_id=?", Long.class, principalId)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT created_at FROM authorization_grant "
                + "WHERE principal_id=?", Timestamp.class, principalId)).isEqualTo(createdAt);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_name='authorization_grant' AND column_name='updated_at'", Long.class)).isZero();
    }

    @Test
    void exactDimensionsAndPrincipalEachProduceDistinctRows() {
        List<AuthorizationGrant> grants = List.of(
                execute(principalId, "lifeos", "lifeos"),
                execute(principalId, "logos", "lifeos"),
                execute(principalId, "lifeos", "other"),
                new AuthorizationGrant(principal(principalId), AuthorizationOperation.PROGRESSION_EXECUTION_READ,
                        Optional.of(new AuthorizationSource("lifeos")), Optional.empty()),
                new AuthorizationGrant(principal(principalId), AuthorizationOperation.PROGRESSION_HISTORY_READ,
                        Optional.empty(), Optional.of(new AuthorizationNamespace("lifeos"))),
                execute(otherPrincipalId, "lifeos", "lifeos"));
        grants.forEach(grant -> assertThat(store.insertIfAbsent(grant)).isEqualTo(InsertResult.CREATED));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM authorization_grant", Long.class)).isEqualTo(6L);
    }

    @Test
    void concurrentExactRetriesCreateOneRowAndReturnPersistenceFacts() throws Exception {
        AuthorizationGrant grant = new AuthorizationGrant(principal(principalId),
                AuthorizationOperation.PROGRESSION_EXECUTION_READ,
                Optional.of(new AuthorizationSource("lifeos")), Optional.empty());
        int workers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<InsertResult>> results = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return store.insertIfAbsent(grant);
                }));
            }
            ready.await();
            start.countDown();
            List<InsertResult> values = new ArrayList<>();
            for (Future<InsertResult> result : results) values.add(result.get());
            assertThat(values.stream().filter(value -> value == InsertResult.CREATED)).hasSize(1);
            assertThat(values.stream().filter(value -> value == InsertResult.ALREADY_EXISTS)).hasSize(workers - 1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM authorization_grant", Long.class)).isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void trustLifecycleAndCredentialRotationDoNotChangeGrant() throws Exception {
        AuthorizationGrant grant = execute(principalId, "lifeos", "lifeos");
        store.insertIfAbsent(grant);
        Timestamp createdAt = jdbc.queryForObject("SELECT created_at FROM authorization_grant "
                + "WHERE principal_id=?", Timestamp.class, principalId);

        insertCredential("kid-a");
        jdbc.update("UPDATE workload_signing_key SET lifecycle_status='ACTIVE', activated_at=? "
                        + "WHERE workload_principal_id=? AND kid='kid-a'",
                Timestamp.from(Instant.now()), principalDbId);
        insertCredential("kid-b");
        jdbc.update("UPDATE workload_signing_key SET lifecycle_status='ACTIVE', activated_at=? "
                        + "WHERE workload_principal_id=? AND kid='kid-b'",
                Timestamp.from(Instant.now()), principalDbId);
        jdbc.update("UPDATE workload_signing_key SET lifecycle_status='RETIRED', retired_at=? "
                        + "WHERE workload_principal_id=? AND kid='kid-a'",
                Timestamp.from(Instant.now()), principalDbId);
        assertGrantUnchanged(grant, createdAt);

        jdbc.update("UPDATE workload_principal SET lifecycle_status='DISABLED', disabled_at=? WHERE id=?",
                Timestamp.from(Instant.now()), principalDbId);
        assertGrantUnchanged(grant, createdAt);
        jdbc.update("UPDATE workload_principal SET lifecycle_status='ACTIVE', disabled_at=NULL WHERE id=?", principalDbId);
        assertGrantUnchanged(grant, createdAt);
        jdbc.update("UPDATE workload_principal SET lifecycle_status='REVOKED', revoked_at=? WHERE id=?",
                Timestamp.from(Instant.now()), principalDbId);
        assertGrantUnchanged(grant, createdAt);
    }

    private void assertGrantUnchanged(AuthorizationGrant grant, Timestamp createdAt) {
        assertThat(store.findExact(grant)).contains(grant);
        assertThat(jdbc.queryForObject("SELECT created_at FROM authorization_grant "
                + "WHERE principal_id=?", Timestamp.class, principalId)).isEqualTo(createdAt);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM authorization_grant "
                + "WHERE principal_id=?", Long.class, principalId)).isEqualTo(1L);
    }

    private void createPrincipal(UUID dbId, String stableId, String principalIssuer) {
        jdbc.update("""
                INSERT INTO workload_principal (id, principal_type, principal_id, issuer, lifecycle_status)
                VALUES (?, 'WORKLOAD', ?, ?, 'ACTIVE')
                """, dbId, stableId, principalIssuer);
    }

    private void insertCredential(String kid) throws Exception {
        var keys = WorkloadTestKeys.ec("secp256r1");
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO workload_signing_key
                    (workload_principal_id, kid, algorithm, public_key_pem, public_key_spki_sha256,
                     lifecycle_status, not_before)
                VALUES (?, ?, 'ES256', ?, ?, 'PENDING', ?)
                """, principalDbId, kid, WorkloadTestKeys.publicPem(keys),
                MessageDigest.getInstance("SHA-256").digest(keys.getPublic().getEncoded()), Timestamp.from(now));
    }

    private AuthorizationGrant execute(String stableId, String source, String namespace) {
        return new AuthorizationGrant(principal(stableId), AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.of(new AuthorizationSource(source)), Optional.of(new AuthorizationNamespace(namespace)));
    }

    private AuthorizationPrincipal principal(String stableId) {
        return new AuthorizationPrincipal(PrincipalType.WORKLOAD, stableId);
    }
}
