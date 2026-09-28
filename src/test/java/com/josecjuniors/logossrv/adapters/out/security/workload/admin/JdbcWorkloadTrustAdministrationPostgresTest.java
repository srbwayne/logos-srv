package com.josecjuniors.logossrv.adapters.out.security.workload.admin;

import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.workload.admin.application.WorkloadTrustAdministrationService;
import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.InvalidWorkloadPublicKeyException;
import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.TrustAdministrationError;
import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.TrustAdministrationException;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore.RegistrationResult;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationActor;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationActorType;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationReason;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAuthenticationService;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionVerificationException;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadCredentialLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalLifecycle;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import com.josecjuniors.logossrv.support.test.WorkloadTestKeys;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.AUDIENCE;
import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.ISSUER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FreshPostgresIntegrationTest
class JdbcWorkloadTrustAdministrationPostgresTest {
    private static final TrustAdministrationActor ACTOR =
            new TrustAdministrationActor(TrustAdministrationActorType.OPERATOR, "f1f-test-operator");
    private static final TrustAdministrationReason REASON = new TrustAdministrationReason("controlled test operation");

    @Autowired JdbcTemplate jdbc;
    @Autowired WorkloadTrustAdministrationService administration;
    @Autowired WorkloadAuthenticationService authentication;
    @Autowired Clock clock;

    @BeforeEach
    @AfterEach
    void cleanTrustState() {
        jdbc.update("DELETE FROM workload_assertion_replay");
        jdbc.update("DELETE FROM workload_trust_audit_event");
        jdbc.update("DELETE FROM workload_signing_key");
        jdbc.update("DELETE FROM workload_principal");
    }

    @Test
    void principalRegistrationRetriesAreNoOpsAcrossLifecycleAndConflictsAreTyped() {
        var principal = new WorkloadPrincipalId("f1f-principal");
        var issuer = new WorkloadIssuer("urn:akume:test:f1f-principal");
        var registration = new WorkloadTrustAdministrationService.RegisterPrincipalCommand(principal, issuer, ACTOR, REASON);
        assertThat(administration.registerPrincipal(registration)).isEqualTo(RegistrationResult.CREATED);
        assertThat(administration.registerPrincipal(registration)).isEqualTo(RegistrationResult.NO_OP);
        assertAuditCount(principal.value(), "WORKLOAD_REGISTERED", 1);
        Instant createdAt = principalUpdatedAt(principal.value());
        assertThat(createdAt.getNano() % 1_000).isZero();
        assertThat(jdbc.queryForObject("SELECT created_at FROM workload_principal WHERE principal_id=?",
                Timestamp.class, principal.value()).toInstant()).isEqualTo(createdAt);
        assertThat(jdbc.queryForObject("SELECT occurred_at FROM workload_trust_audit_event WHERE action='WORKLOAD_REGISTERED'",
                Timestamp.class).toInstant()).isEqualTo(createdAt);

        administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.DISABLED));
        assertThat(administration.registerPrincipal(registration)).isEqualTo(RegistrationResult.NO_OP);
        assertPrincipal(principal.value(), "DISABLED", true, false);
        administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.REVOKED));
        assertThat(administration.registerPrincipal(registration)).isEqualTo(RegistrationResult.NO_OP);
        assertPrincipal(principal.value(), "REVOKED", true, true);

        assertError(TrustAdministrationError.PRINCIPAL_IDENTITY_CONFLICT,
                () -> administration.registerPrincipal(new WorkloadTrustAdministrationService.RegisterPrincipalCommand(
                        principal, new WorkloadIssuer("urn:akume:test:different-issuer"), ACTOR, REASON)));
        var other = new WorkloadPrincipalId("f1f-other-principal");
        assertError(TrustAdministrationError.ISSUER_CONFLICT,
                () -> administration.registerPrincipal(new WorkloadTrustAdministrationService.RegisterPrincipalCommand(
                        other, issuer, ACTOR, REASON)));
    }

    @Test
    void principalLifecyclePreservesAndClearsTimestampsAndAuditsOnlyMutations() {
        WorkloadPrincipalId principal = createPrincipal("life-cycle", "urn:akume:test:f1f-life-cycle");
        Instant firstUpdated = principalUpdatedAt(principal.value());

        administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.DISABLED));
        Instant disabledAt = timestamp("workload_principal", "disabled_at", principal.value());
        assertThat(disabledAt).isNotNull();
        assertThat(timestamp("workload_principal", "revoked_at", principal.value())).isNull();
        administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.DISABLED));
        assertThat(principalUpdatedAt(principal.value())).isEqualTo(disabledAt);

        administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.REVOKED));
        assertThat(timestamp("workload_principal", "disabled_at", principal.value())).isEqualTo(disabledAt);
        assertThat(timestamp("workload_principal", "revoked_at", principal.value())).isNotNull();
        assertError(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION,
                () -> administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.ACTIVE)));
        assertThat(auditCount(principal.value())).isEqualTo(3);
        assertThat(firstUpdated).isNotNull();
    }

    @Test
    void credentialRegistrationUsesCanonicalSpkiFingerprintAndRetrySurvivesLifecycleChanges() throws Exception {
        WorkloadPrincipalId principal = createPrincipal("key-owner", "urn:akume:test:f1f-key-owner");
        KeyPair keyPair = WorkloadTestKeys.ec("secp256r1");
        String pem = WorkloadTestKeys.publicPem(keyPair);
        Instant before = Instant.parse("2026-07-01T01:02:03.123456789Z");
        Instant after = Instant.parse("2027-07-01T01:02:03.987654321Z");
        var register = keyCommand(principal, "kid-one", pem, before, after);

        assertThat(administration.registerCredential(register)).isEqualTo(RegistrationResult.CREATED);
        assertThat(administration.registerCredential(register)).isEqualTo(RegistrationResult.NO_OP);
        assertThat(keyStatus(principal.value(), "kid-one")).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT algorithm FROM workload_signing_key WHERE kid='kid-one'", String.class))
                .isEqualTo("ES256");
        byte[] fingerprint = MessageDigest.getInstance("SHA-256").digest(keyPair.getPublic().getEncoded());
        assertThat(jdbc.queryForObject("SELECT public_key_spki_sha256 FROM workload_signing_key WHERE kid='kid-one'",
                byte[].class)).containsExactly(fingerprint);
        assertThat(keyTimestamp(principal.value(), "kid-one", "not_before"))
                .isEqualTo(before.truncatedTo(ChronoUnit.MICROS));
        assertThat(keyTimestamp(principal.value(), "kid-one", "not_after"))
                .isEqualTo(after.truncatedTo(ChronoUnit.MICROS));

        administration.changeCredentialLifecycle(credentialCommand(principal, "kid-one", WorkloadCredentialLifecycle.ACTIVE));
        assertThat(administration.registerCredential(register)).isEqualTo(RegistrationResult.NO_OP);
        assertThat(keyStatus(principal.value(), "kid-one")).isEqualTo("ACTIVE");
        administration.changeCredentialLifecycle(credentialCommand(principal, "kid-one", WorkloadCredentialLifecycle.REVOKED));
        assertThat(administration.registerCredential(register)).isEqualTo(RegistrationResult.NO_OP);
        assertThat(keyStatus(principal.value(), "kid-one")).isEqualTo("REVOKED");
        assertAuditCount(principal.value(), "CREDENTIAL_REGISTERED", 1);
        assertThat(jdbc.queryForObject("SELECT metadata::text FROM workload_trust_audit_event "
                + "WHERE action='CREDENTIAL_REGISTERED'", String.class)).contains(HexFormat.of().formatHex(fingerprint))
                .doesNotContain("BEGIN PUBLIC KEY", "PRIVATE KEY");
        assertThat(jdbc.queryForObject("SELECT actor_type || ':' || actor_id || ':' || reason "
                + "FROM workload_trust_audit_event WHERE action='CREDENTIAL_REGISTERED'", String.class))
                .isEqualTo("OPERATOR:f1f-test-operator:controlled test operation");
        assertThat(jdbc.queryForObject("SELECT occurred_at FROM workload_trust_audit_event "
                + "WHERE action='CREDENTIAL_REGISTERED'", Timestamp.class).toInstant())
                .isEqualTo(keyTimestamp(principal.value(), "kid-one", "created_at"));
    }

    @Test
    void equivalentKeyMaterialIsIdempotentAndKidOrFingerprintReuseConflicts() throws Exception {
        WorkloadPrincipalId principal = createPrincipal("collision-owner", "urn:akume:test:f1f-collision");
        WorkloadPrincipalId otherPrincipal = createPrincipal("collision-other", "urn:akume:test:f1f-collision-other");
        KeyPair first = WorkloadTestKeys.ec("secp256r1");
        String pem = WorkloadTestKeys.publicPem(first);
        Instant before = Instant.parse("2026-07-01T00:00:00.123456Z");
        assertThat(administration.registerCredential(keyCommand(principal, "same-kid", pem, before, null)))
                .isEqualTo(RegistrationResult.CREATED);
        String equivalentPem = "-----BEGIN PUBLIC KEY-----\n"
                + java.util.Base64.getMimeEncoder(48, new byte[]{'\n'}).encodeToString(first.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        assertThat(administration.registerCredential(keyCommand(principal, "same-kid", equivalentPem, before, null)))
                .isEqualTo(RegistrationResult.NO_OP);

        KeyPair different = WorkloadTestKeys.ec("secp256r1");
        assertError(TrustAdministrationError.KID_CONFLICT,
                () -> administration.registerCredential(keyCommand(principal, "same-kid",
                        WorkloadTestKeys.publicPem(different), before, null)));
        assertError(TrustAdministrationError.FINGERPRINT_CONFLICT,
                () -> administration.registerCredential(keyCommand(principal, "another-kid", pem, before, null)));
        assertError(TrustAdministrationError.FINGERPRINT_CONFLICT,
                () -> administration.registerCredential(keyCommand(otherPrincipal, "other-kid", pem, before, null)));
    }

    @Test
    void invalidPublicKeyMaterialIsTypedAndNeverPersisted() throws Exception {
        WorkloadPrincipalId principal = createPrincipal("invalid-key", "urn:akume:test:f1f-invalid-key");
        List<String> invalid = List.of("broken", WorkloadTestKeys.publicPem(WorkloadTestKeys.rsa()),
                WorkloadTestKeys.privatePem(WorkloadTestKeys.ec("secp256r1")),
                WorkloadTestKeys.publicPem(WorkloadTestKeys.ec("secp384r1")),
                WorkloadTestKeys.publicPem(WorkloadTestKeys.ec("secp256r1")) + WorkloadTestKeys.publicPem(WorkloadTestKeys.ec("secp256r1")));
        int i = 0;
        for (String pem : invalid) {
            assertThatThrownBy(() -> administration.registerCredential(keyCommand(principal,
                    "invalid-" + UUID.randomUUID(), pem, clock.instant(), null)))
                    .isInstanceOf(InvalidWorkloadPublicKeyException.class)
                    .extracting(error -> ((TrustAdministrationException) error).error())
                    .isEqualTo(TrustAdministrationError.INVALID_PUBLIC_KEY);
            i++;
        }
        assertThat(i).isEqualTo(invalid.size());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_signing_key WHERE workload_principal_id = "
                + "(SELECT id FROM workload_principal WHERE principal_id=?)", Long.class, principal.value())).isZero();
        assertAuditCount(principal.value(), "CREDENTIAL_REGISTERED", 0);
    }

    @Test
    void registrationEnforcesMicrosecondWindowAndAllowsPendingOnDisabledOnly() throws Exception {
        WorkloadPrincipalId disabled = createPrincipal("disabled-registration", "urn:akume:test:f1f-disabled");
        administration.changePrincipalLifecycle(principalCommand(disabled, WorkloadPrincipalLifecycle.DISABLED));
        assertThat(administration.registerCredential(keyCommand(disabled, "pending-disabled",
                WorkloadTestKeys.publicPem(WorkloadTestKeys.ec("secp256r1")), clock.instant(), null)))
                .isEqualTo(RegistrationResult.CREATED);
        WorkloadPrincipalId revoked = createPrincipal("revoked-registration", "urn:akume:test:f1f-revoked");
        administration.changePrincipalLifecycle(principalCommand(revoked, WorkloadPrincipalLifecycle.REVOKED));
        String deniedKey = WorkloadTestKeys.publicPem(WorkloadTestKeys.ec("secp256r1"));
        assertError(TrustAdministrationError.PRINCIPAL_REVOKED,
                () -> administration.registerCredential(keyCommand(revoked, "denied",
                        deniedKey, clock.instant(), null)));
    }

    @Test
    void credentialLifecycleTerminalStatesAndTimestampSemanticsAreEnforced() throws Exception {
        WorkloadPrincipalId principal = createPrincipal("credential-states", "urn:akume:test:f1f-credential-states");
        registerKey(principal, "pending", WorkloadTestKeys.ec("secp256r1"));
        administration.changeCredentialLifecycle(credentialCommand(principal, "pending", WorkloadCredentialLifecycle.REVOKED));
        assertThat(keyTimestamp(principal.value(), "pending", "activated_at")).isNull();
        assertThat(keyTimestamp(principal.value(), "pending", "revoked_at")).isNotNull();
        assertError(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION,
                () -> administration.changeCredentialLifecycle(credentialCommand(principal, "pending", WorkloadCredentialLifecycle.RETIRED)));
        administration.changeCredentialLifecycle(credentialCommand(principal, "pending", WorkloadCredentialLifecycle.REVOKED));

        registerKey(principal, "active-revoke", WorkloadTestKeys.ec("secp256r1"));
        administration.changeCredentialLifecycle(credentialCommand(principal, "active-revoke", WorkloadCredentialLifecycle.ACTIVE));
        Instant activatedAt = keyTimestamp(principal.value(), "active-revoke", "activated_at");
        administration.changeCredentialLifecycle(credentialCommand(principal, "active-revoke", WorkloadCredentialLifecycle.REVOKED));
        assertThat(keyTimestamp(principal.value(), "active-revoke", "activated_at")).isEqualTo(activatedAt);
        assertError(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION,
                () -> administration.changeCredentialLifecycle(credentialCommand(principal, "active-revoke", WorkloadCredentialLifecycle.RETIRED)));

        registerKey(principal, "active-retire", WorkloadTestKeys.ec("secp256r1"));
        administration.changeCredentialLifecycle(credentialCommand(principal, "active-retire", WorkloadCredentialLifecycle.ACTIVE));
        administration.changeCredentialLifecycle(credentialCommand(principal, "active-retire", WorkloadCredentialLifecycle.RETIRED));
        assertThat(keyTimestamp(principal.value(), "active-retire", "activated_at")).isNotNull();
        assertThat(keyTimestamp(principal.value(), "active-retire", "revoked_at")).isNull();
        assertThat(keyTimestamp(principal.value(), "active-retire", "retired_at")).isNotNull();
        assertError(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION,
                () -> administration.changeCredentialLifecycle(credentialCommand(principal, "active-retire", WorkloadCredentialLifecycle.REVOKED)));
        administration.changeCredentialLifecycle(credentialCommand(principal, "active-retire", WorkloadCredentialLifecycle.RETIRED));
    }

    @Test
    void auditFailureRollsBackTrustMutation() throws Exception {
        WorkloadPrincipalId principal = createPrincipal("audit-rollback", "urn:akume:test:f1f-audit-rollback");
        jdbc.execute("CREATE OR REPLACE FUNCTION f1f_fail_audit() RETURNS trigger LANGUAGE plpgsql AS "
                + "$$ BEGIN RAISE EXCEPTION 'test audit failure'; END $$");
        jdbc.execute("CREATE TRIGGER f1f_fail_audit_trigger BEFORE INSERT ON workload_trust_audit_event "
                + "FOR EACH ROW EXECUTE FUNCTION f1f_fail_audit()");
        try {
            assertError(TrustAdministrationError.PERSISTENCE_FAILURE,
                    () -> administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.DISABLED)));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS f1f_fail_audit_trigger ON workload_trust_audit_event");
            jdbc.execute("DROP FUNCTION IF EXISTS f1f_fail_audit()");
        }
        assertPrincipal(principal.value(), "ACTIVE", false, false);
        assertAuditCount(principal.value(), "WORKLOAD_DISABLED", 0);
    }

    @Test
    void concurrentPrincipalAndCredentialRegistrationAreDeterministic() throws Exception {
        WorkloadPrincipalId principal = new WorkloadPrincipalId("race-principal");
        WorkloadIssuer issuer = new WorkloadIssuer("urn:akume:test:f1f-race-principal");
        var principalCommand = new WorkloadTrustAdministrationService.RegisterPrincipalCommand(principal, issuer, ACTOR, REASON);
        List<RegistrationResult> principalResults = concurrent(8,
                () -> administration.registerPrincipal(principalCommand));
        assertThat(principalResults).containsExactlyInAnyOrder(RegistrationResult.CREATED,
                RegistrationResult.NO_OP, RegistrationResult.NO_OP, RegistrationResult.NO_OP,
                RegistrationResult.NO_OP, RegistrationResult.NO_OP, RegistrationResult.NO_OP, RegistrationResult.NO_OP);
        assertAuditCount(principal.value(), "WORKLOAD_REGISTERED", 1);

        KeyPair key = WorkloadTestKeys.ec("secp256r1");
        var keyCommand = keyCommand(principal, "race-kid", WorkloadTestKeys.publicPem(key), clock.instant(), null);
        List<RegistrationResult> keyResults = concurrent(8, () -> administration.registerCredential(keyCommand));
        assertThat(keyResults).containsExactlyInAnyOrder(RegistrationResult.CREATED,
                RegistrationResult.NO_OP, RegistrationResult.NO_OP, RegistrationResult.NO_OP,
                RegistrationResult.NO_OP, RegistrationResult.NO_OP, RegistrationResult.NO_OP, RegistrationResult.NO_OP);
        assertAuditCount(principal.value(), "CREDENTIAL_REGISTERED", 1);
    }

    @Test
    void concurrentRetireAndRevokeChooseOneTerminalStateWithoutOverwrite() throws Exception {
        WorkloadPrincipalId principal = createPrincipal("terminal-race", "urn:akume:test:f1f-terminal-race");
        registerKey(principal, "terminal-kid", WorkloadTestKeys.ec("secp256r1"));
        administration.changeCredentialLifecycle(credentialCommand(principal, "terminal-kid", WorkloadCredentialLifecycle.ACTIVE));
        var outcomes = concurrentOutcomes(
                () -> administration.changeCredentialLifecycle(credentialCommand(principal, "terminal-kid", WorkloadCredentialLifecycle.RETIRED)),
                () -> administration.changeCredentialLifecycle(credentialCommand(principal, "terminal-kid", WorkloadCredentialLifecycle.REVOKED)));
        assertThat(outcomes.stream().filter(Outcome::isEmpty).count()).isEqualTo(1);
        String finalState = keyStatus(principal.value(), "terminal-kid");
        assertThat(finalState).isIn("RETIRED", "REVOKED");
        String rejected = finalState.equals("RETIRED") ? "CREDENTIAL_REVOKED" : "CREDENTIAL_RETIRED";
        assertAuditCount(principal.value(), rejected, 0);
    }

    @Test
    void conflictingPrincipalAndCredentialRegistrationsHaveOneDeterministicWinner() throws Exception {
        WorkloadPrincipalId principal = new WorkloadPrincipalId("identity-conflict-race");
        var sameId = concurrentOutcomes(
                () -> administration.registerPrincipal(new WorkloadTrustAdministrationService.RegisterPrincipalCommand(
                        principal, new WorkloadIssuer("urn:akume:test:identity-race-a"), ACTOR, REASON)),
                () -> administration.registerPrincipal(new WorkloadTrustAdministrationService.RegisterPrincipalCommand(
                        principal, new WorkloadIssuer("urn:akume:test:identity-race-b"), ACTOR, REASON)));
        assertExactlyOneConflict(sameId, TrustAdministrationError.PRINCIPAL_IDENTITY_CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_principal WHERE principal_id=?", Long.class,
                principal.value())).isEqualTo(1);
        assertAuditCount(principal.value(), "WORKLOAD_REGISTERED", 1);

        String sharedIssuer = "urn:akume:test:issuer-race";
        var sameIssuer = concurrentOutcomes(
                () -> administration.registerPrincipal(new WorkloadTrustAdministrationService.RegisterPrincipalCommand(
                        new WorkloadPrincipalId("issuer-race-a"), new WorkloadIssuer(sharedIssuer), ACTOR, REASON)),
                () -> administration.registerPrincipal(new WorkloadTrustAdministrationService.RegisterPrincipalCommand(
                        new WorkloadPrincipalId("issuer-race-b"), new WorkloadIssuer(sharedIssuer), ACTOR, REASON)));
        assertExactlyOneConflict(sameIssuer, TrustAdministrationError.ISSUER_CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_principal WHERE issuer=?", Long.class,
                sharedIssuer)).isEqualTo(1);

        WorkloadPrincipalId keyOwner = createPrincipal("kid-race-owner", "urn:akume:test:kid-race-owner");
        KeyPair first = WorkloadTestKeys.ec("secp256r1");
        KeyPair second = WorkloadTestKeys.ec("secp256r1");
        var sameKid = concurrentOutcomes(
                () -> administration.registerCredential(keyCommand(keyOwner, "racing-kid",
                        WorkloadTestKeys.publicPem(first), clock.instant(), null)),
                () -> administration.registerCredential(keyCommand(keyOwner, "racing-kid",
                        WorkloadTestKeys.publicPem(second), clock.instant(), null)));
        assertExactlyOneConflict(sameKid, TrustAdministrationError.KID_CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_signing_key WHERE kid='racing-kid'",
                Long.class)).isEqualTo(1);
        assertAuditCount(keyOwner.value(), "CREDENTIAL_REGISTERED", 1);

        KeyPair shared = WorkloadTestKeys.ec("secp256r1");
        String sharedPem = WorkloadTestKeys.publicPem(shared);
        var sameFingerprintDifferentKids = concurrentOutcomes(
                () -> administration.registerCredential(keyCommand(keyOwner, "fingerprint-race-a",
                        sharedPem, clock.instant(), null)),
                () -> administration.registerCredential(keyCommand(keyOwner, "fingerprint-race-b",
                        sharedPem, clock.instant(), null)));
        assertExactlyOneConflict(sameFingerprintDifferentKids, TrustAdministrationError.FINGERPRINT_CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_signing_key WHERE public_key_spki_sha256=?",
                Long.class, MessageDigest.getInstance("SHA-256").digest(shared.getPublic().getEncoded()))).isEqualTo(1);

        WorkloadPrincipalId otherOwner = createPrincipal("fingerprint-race-other", "urn:akume:test:fingerprint-race-other");
        KeyPair crossPrincipalShared = WorkloadTestKeys.ec("secp256r1");
        String crossPrincipalPem = WorkloadTestKeys.publicPem(crossPrincipalShared);
        var crossPrincipalFingerprint = concurrentOutcomes(
                () -> administration.registerCredential(keyCommand(keyOwner, "cross-fingerprint-a",
                        crossPrincipalPem, clock.instant(), null)),
                () -> administration.registerCredential(keyCommand(otherOwner, "cross-fingerprint-b",
                        crossPrincipalPem, clock.instant(), null)));
        assertExactlyOneConflict(crossPrincipalFingerprint, TrustAdministrationError.FINGERPRINT_CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_signing_key WHERE public_key_spki_sha256=?",
                Long.class, MessageDigest.getInstance("SHA-256").digest(crossPrincipalShared.getPublic().getEncoded())))
                .isEqualTo(1);
    }

    @Test
    void principalLifecycleRacesSerializeAndRevocationRemainsTerminal() throws Exception {
        WorkloadPrincipalId principal = createPrincipal("principal-race", "urn:akume:test:principal-race");
        var disableEnable = concurrentOutcomes(
                () -> administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.DISABLED)),
                () -> administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.ACTIVE)));
        assertThat(disableEnable).allSatisfy(this::assertNoUnexpectedConcurrencyFailure);
        assertThat(principalStatus(principal.value())).isIn("ACTIVE", "DISABLED");
        assertThat(auditCount(principal.value())).isBetween(2L, 3L);

        WorkloadPrincipalId disabled = createPrincipal("disable-revoke-race", "urn:akume:test:disable-revoke-race");
        var disableRevoke = concurrentOutcomes(
                () -> administration.changePrincipalLifecycle(principalCommand(disabled, WorkloadPrincipalLifecycle.DISABLED)),
                () -> administration.changePrincipalLifecycle(principalCommand(disabled, WorkloadPrincipalLifecycle.REVOKED)));
        assertThat(principalStatus(disabled.value())).isEqualTo("REVOKED");
        assertThat(disableRevoke).allSatisfy(this::assertNoUnexpectedConcurrencyFailure);
        assertThat(auditCount(disabled.value())).isIn(2L, 3L);

        WorkloadPrincipalId disabledForEnable = createPrincipal("enable-revoke-race", "urn:akume:test:enable-revoke-race");
        administration.changePrincipalLifecycle(principalCommand(disabledForEnable, WorkloadPrincipalLifecycle.DISABLED));
        var enableRevoke = concurrentOutcomes(
                () -> administration.changePrincipalLifecycle(principalCommand(disabledForEnable, WorkloadPrincipalLifecycle.ACTIVE)),
                () -> administration.changePrincipalLifecycle(principalCommand(disabledForEnable, WorkloadPrincipalLifecycle.REVOKED)));
        assertThat(principalStatus(disabledForEnable.value())).isEqualTo("REVOKED");
        assertThat(enableRevoke).allSatisfy(this::assertNoUnexpectedConcurrencyFailure);
        assertError(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION,
                () -> administration.changePrincipalLifecycle(principalCommand(disabledForEnable, WorkloadPrincipalLifecycle.ACTIVE)));
    }

    @Test
    void canonicalAuthenticationHonorsPrincipalLifecycleAndRotationWithoutKeyCascade() throws Exception {
        WorkloadPrincipalId principal = new WorkloadPrincipalId("lifeos");
        var registration = new WorkloadTrustAdministrationService.RegisterPrincipalCommand(principal,
                new WorkloadIssuer(ISSUER), ACTOR, REASON);
        administration.registerPrincipal(registration);
        KeyPair keyA = WorkloadTestKeys.ec("secp256r1");
        KeyPair keyB = WorkloadTestKeys.ec("secp256r1");
        registerKey(principal, "kid-a", keyA);
        registerKey(principal, "kid-b", keyB);
        administration.changeCredentialLifecycle(credentialCommand(principal, "kid-a", WorkloadCredentialLifecycle.ACTIVE));
        administration.changeCredentialLifecycle(credentialCommand(principal, "kid-b", WorkloadCredentialLifecycle.ACTIVE));
        assertAuthenticated(assertion("kid-a", keyA));
        assertAuthenticated(assertion("kid-b", keyB));

        administration.changeCredentialLifecycle(credentialCommand(principal, "kid-a", WorkloadCredentialLifecycle.RETIRED));
        assertRejected(assertion("kid-a", keyA));
        assertAuthenticated(assertion("kid-b", keyB));
        administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.DISABLED));
        assertRejected(assertion("kid-b", keyB));
        administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.ACTIVE));
        assertAuthenticated(assertion("kid-b", keyB));
        administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.REVOKED));
        assertRejected(assertion("kid-b", keyB));
        assertThat(keyStatus(principal.value(), "kid-b")).isEqualTo("ACTIVE");
    }

    @Test
    void concurrentPrincipalRevocationAndKeyActivationNeverLeaveAuthenticationEligiblePrincipal() throws Exception {
        WorkloadPrincipalId principal = new WorkloadPrincipalId("lifeos");
        administration.registerPrincipal(new WorkloadTrustAdministrationService.RegisterPrincipalCommand(
                principal, new WorkloadIssuer(ISSUER), ACTOR, REASON));
        KeyPair key = WorkloadTestKeys.ec("secp256r1");
        registerKey(principal, "cross-race", key);
        var outcomes = concurrentOutcomes(
                () -> administration.changeCredentialLifecycle(credentialCommand(principal, "cross-race", WorkloadCredentialLifecycle.ACTIVE)),
                () -> administration.changePrincipalLifecycle(principalCommand(principal, WorkloadPrincipalLifecycle.REVOKED)));
        assertThat(outcomes.stream().filter(Outcome::isEmpty).count()).isGreaterThanOrEqualTo(1);
        assertThat(principalStatus(principal.value())).isEqualTo("REVOKED");
        assertRejected(assertion("cross-race", key));
    }

    private void assertAuthenticated(String token) {
        AuthenticatedPrincipal authenticated = authentication.authenticate(token);
        assertThat(authenticated.principalId()).isEqualTo("lifeos");
    }

    private void assertRejected(String token) {
        assertThatThrownBy(() -> authentication.authenticate(token))
                .isInstanceOf(WorkloadAssertionVerificationException.class);
    }

    private String assertion(String kid, KeyPair key) {
        Instant now = clock.instant();
        return Jwts.builder().header().type(WorkloadAssertionProfile.TYPE).keyId(kid).and()
                .issuer(ISSUER).subject("lifeos").audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now.minusSeconds(1))).expiration(Date.from(now.plusSeconds(59)))
                .id(UUID.randomUUID().toString()).signWith(key.getPrivate(), Jwts.SIG.ES256).compact();
    }

    private WorkloadPrincipalId createPrincipal(String id, String issuer) {
        WorkloadPrincipalId principal = new WorkloadPrincipalId(id);
        administration.registerPrincipal(new WorkloadTrustAdministrationService.RegisterPrincipalCommand(
                principal, new WorkloadIssuer(issuer), ACTOR, REASON));
        return principal;
    }

    private void registerKey(WorkloadPrincipalId principal, String kid, KeyPair key) {
        administration.registerCredential(keyCommand(principal, kid, WorkloadTestKeys.publicPem(key), clock.instant(), null));
    }

    private static WorkloadTrustAdministrationService.RegisterCredentialCommand keyCommand(
            WorkloadPrincipalId principal, String kid, String pem, Instant before, Instant after) {
        return new WorkloadTrustAdministrationService.RegisterCredentialCommand(principal,
                new WorkloadKeyId(kid), pem, before, after, ACTOR, REASON);
    }

    private static WorkloadTrustAdministrationService.PrincipalLifecycleCommand principalCommand(
            WorkloadPrincipalId principal, WorkloadPrincipalLifecycle target) {
        return new WorkloadTrustAdministrationService.PrincipalLifecycleCommand(principal, target, ACTOR, REASON);
    }

    private static WorkloadTrustAdministrationService.CredentialLifecycleCommand credentialCommand(
            WorkloadPrincipalId principal, String kid, WorkloadCredentialLifecycle target) {
        return new WorkloadTrustAdministrationService.CredentialLifecycleCommand(
                principal, new WorkloadKeyId(kid), target, ACTOR, REASON);
    }

    private void assertError(TrustAdministrationError expected, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(TrustAdministrationException.class)
                .extracting(error -> ((TrustAdministrationException) error).error()).isEqualTo(expected);
    }

    private void assertPrincipal(String principalId, String status, boolean disabled, boolean revoked) {
        assertThat(principalStatus(principalId)).isEqualTo(status);
        assertThat(timestamp("workload_principal", "disabled_at", principalId) != null).isEqualTo(disabled);
        assertThat(timestamp("workload_principal", "revoked_at", principalId) != null).isEqualTo(revoked);
    }

    private String principalStatus(String principalId) {
        return jdbc.queryForObject("SELECT lifecycle_status FROM workload_principal WHERE principal_id = ?",
                String.class, principalId);
    }

    private String keyStatus(String principalId, String kid) {
        return jdbc.queryForObject("SELECT lifecycle_status FROM workload_signing_key "
                + "WHERE workload_principal_id=(SELECT id FROM workload_principal WHERE principal_id=?) AND kid=?",
                String.class, principalId, kid);
    }

    private Instant principalUpdatedAt(String principalId) {
        return jdbc.queryForObject("SELECT updated_at FROM workload_principal WHERE principal_id=?",
                Timestamp.class, principalId).toInstant();
    }

    private Instant timestamp(String table, String column, String principalId) {
        String sql = table.equals("workload_principal")
                ? "SELECT " + column + " FROM workload_principal WHERE principal_id=?"
                : "SELECT " + column + " FROM workload_signing_key WHERE workload_principal_id="
                + "(SELECT id FROM workload_principal WHERE principal_id=?)";
        Timestamp value = jdbc.queryForObject(sql, Timestamp.class, principalId);
        return value == null ? null : value.toInstant();
    }

    private Instant keyTimestamp(String principalId, String kid, String column) {
        Timestamp value = jdbc.queryForObject("SELECT " + column + " FROM workload_signing_key "
                + "WHERE workload_principal_id=(SELECT id FROM workload_principal WHERE principal_id=?) AND kid=?",
                Timestamp.class, principalId, kid);
        return value == null ? null : value.toInstant();
    }

    private long auditCount(String principalId) {
        return jdbc.queryForObject("SELECT count(*) FROM workload_trust_audit_event WHERE "
                + "workload_principal_id=(SELECT id FROM workload_principal WHERE principal_id=?)", Long.class, principalId);
    }

    private void assertAuditCount(String principalId, String action, long expected) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_trust_audit_event WHERE "
                + "workload_principal_id=(SELECT id FROM workload_principal WHERE principal_id=?) AND action=?",
                Long.class, principalId, action)).isEqualTo(expected);
    }

    private List<RegistrationResult> concurrent(int workers, CheckedSupplier<RegistrationResult> operation) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<RegistrationResult>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < workers; i++) futures.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timed out");
                return operation.get();
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return futures.stream().map(future -> {
                try { return future.get(45, TimeUnit.SECONDS); }
                catch (Exception exception) { throw new RuntimeException(exception); }
            }).toList();
        } finally {
            pool.shutdownNow();
        }
    }

    private List<Outcome> concurrentOutcomes(CheckedRunnable first, CheckedRunnable second) throws Exception {
        return concurrentOutcomes(List.of(first, second));
    }

    private List<Outcome> concurrentOutcomes(CheckedRunnable... operations) throws Exception {
        return concurrentOutcomes(List.of(operations));
    }

    private List<Outcome> concurrentOutcomes(List<CheckedRunnable> operations) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(operations.size());
        CountDownLatch ready = new CountDownLatch(operations.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = operations.stream().map(operation -> pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timed out");
                try { operation.run(); return new Outcome(null); }
                catch (Throwable error) { return new Outcome(error); }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return futures.stream().map(future -> {
                try { return future.get(45, TimeUnit.SECONDS); }
                catch (Exception exception) { throw new RuntimeException(exception); }
            }).toList();
        } finally {
            pool.shutdownNow();
        }
    }

    private void assertExactlyOneConflict(List<Outcome> outcomes, TrustAdministrationError expected) {
        assertThat(outcomes).hasSize(2);
        assertThat(outcomes.stream().filter(Outcome::isEmpty).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter(outcome -> outcome.error() instanceof TrustAdministrationException ex
                && ex.error() == expected).count()).isEqualTo(1);
    }

    private void assertNoUnexpectedConcurrencyFailure(Outcome outcome) {
        if (outcome.error() != null) {
            assertThat(outcome.error()).isInstanceOf(TrustAdministrationException.class)
                    .extracting(error -> ((TrustAdministrationException) error).error())
                    .isEqualTo(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION);
        }
    }

    @FunctionalInterface private interface CheckedSupplier<T> { T get() throws Exception; }
    @FunctionalInterface private interface CheckedRunnable { void run() throws Exception; }
    private record Outcome(Throwable error) { boolean isEmpty() { return error == null; } }
}
