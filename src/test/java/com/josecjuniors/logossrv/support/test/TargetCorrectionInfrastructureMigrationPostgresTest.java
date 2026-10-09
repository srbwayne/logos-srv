package com.josecjuniors.logossrv.support.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import com.josecjuniors.logossrv.adapters.out.subjectownership.jpa.ProgressionSubjectTargetCorrectionAuthorization;
import com.josecjuniors.logossrv.adapters.out.subjectownership.jpa.ProgressionSubjectTargetCorrectionAuthorizationJpaRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@FreshPostgresIntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=target-correction-infrastructure")
class TargetCorrectionInfrastructureMigrationPostgresTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired Environment environment;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ProgressionSubjectTargetCorrectionAuthorizationJpaRepository authorizationRepository;

    @Test
    void cleanMigrationIncludesV50AndPreservesV49PredecessorUniqueness() {
        assertThat(jdbc.queryForObject("""
                SELECT version FROM flyway_schema_history WHERE success = true
                ORDER BY installed_rank DESC LIMIT 1
                """, String.class)).isEqualTo("50");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '50'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_constraint
                WHERE conrelid = 'progression_subject_identity'::regclass
                  AND conname = 'uq_progression_subject_identity_predecessor'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void v49SchemaUpgradesToV50WithoutRewritingExistingData() throws Exception {
        String baseUrl = withoutCurrentSchema(environment.getRequiredProperty("spring.datasource.url"));
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schema = "logos_tc_r1_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        String isolatedUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }
        try {
            flyway(baseUrl, username, password, schema, "49").migrate();
            assertThat(latestVersion(isolatedUrl, username, password)).isEqualTo("49");
            flyway(baseUrl, username, password, schema, "50").migrate();
            assertThat(latestVersion(isolatedUrl, username, password)).isEqualTo("50");
            try (Connection connection = DriverManager.getConnection(isolatedUrl, username, password);
                 Statement statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT count(*) FROM progression_subject_identity")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
        } finally {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }

    @Test
    void v49UpgradePreservesAnExistingValidReassignment() throws Exception {
        String baseUrl = withoutCurrentSchema(environment.getRequiredProperty("spring.datasource.url"));
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schema = "logos_tc_r1_reassignment_" + UUID.randomUUID().toString().replace("-", "");
        String isolatedUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }
        try {
            flyway(baseUrl, username, password, schema, "49").migrate();
            UUID predecessorId = UUID.randomUUID();
            UUID successorId = UUID.randomUUID();
            UUID oldTargetId = UUID.randomUUID();
            UUID newTargetId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID authorizationId = UUID.randomUUID();
            String externalId = "existing-reassignment-" + UUID.randomUUID();
            seedValidReassignment(isolatedUrl, username, password, predecessorId, successorId,
                    oldTargetId, newTargetId, requestId, authorizationId, externalId);

            flyway(baseUrl, username, password, schema, "50").migrate();

            try (Connection connection = DriverManager.getConnection(isolatedUrl, username, password);
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT count(*) FROM progression_subject_ownership_history
                         WHERE event_type = 'OWNERSHIP_REASSIGNED'
                           AND reassignment_request_id = ? AND reassignment_authorization_id = ?
                         """)) {
                statement.setObject(1, requestId);
                statement.setObject(2, authorizationId);
                try (var result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isEqualTo(1);
                }
            }
            assertThat(latestVersion(isolatedUrl, username, password)).isEqualTo("50");
        } finally {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }

    @Test
    void v49UpgradeAcceptsReassignmentSuccessorAfterReactivationAndVerification() throws Exception {
        String baseUrl = withoutCurrentSchema(environment.getRequiredProperty("spring.datasource.url"));
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schema = "tc_r1_verified_" + UUID.randomUUID().toString().replace("-", "");
        String isolatedUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }
        try {
            flyway(baseUrl, username, password, schema, "49").migrate();
            UUID predecessorId = UUID.randomUUID();
            UUID successorId = UUID.randomUUID();
            UUID originalTargetId = UUID.randomUUID();
            UUID reassignmentTargetId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID authorizationId = UUID.randomUUID();
            String externalId = "verified-reassignment-" + UUID.randomUUID();
            seedValidReassignment(isolatedUrl, username, password, predecessorId, successorId,
                    originalTargetId, reassignmentTargetId, requestId, authorizationId, externalId);

            // The Spring test context is migrated to V50. To exercise the
            // V49->V50 upgrade, persist the same canonical application event
            // snapshots in the isolated V49 schema before running that upgrade.
            reactivateAndVerifySuccessor(isolatedUrl, username, password, successorId, reassignmentTargetId);
            assertThat(latestIdentityState(isolatedUrl, username, password, successorId))
                    .isEqualTo("ACTIVE|VERIFIED|2|" + reassignmentTargetId);

            flyway(baseUrl, username, password, schema, "50").migrate();

            assertThat(latestVersion(isolatedUrl, username, password)).isEqualTo("50");
            assertThat(jsonRow(isolatedUrl, username, password, """
                    SELECT (to_jsonb(event) - 'correction_request_id'
                            - 'target_correction_authorization_id')::text
                    FROM progression_subject_ownership_history event
                    WHERE event.event_type = 'OWNERSHIP_REASSIGNED' AND event.reassignment_request_id = ?
                    """, requestId)).isNotNull();
            assertThat(latestIdentityState(isolatedUrl, username, password, successorId))
                    .isEqualTo("ACTIVE|VERIFIED|2|" + reassignmentTargetId);
        } finally {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }

    @Test
    void v49UpgradeAcceptsReassignmentAfterSuccessorLifecycleAndLaterReassignment() throws Exception {
        String baseUrl = withoutCurrentSchema(environment.getRequiredProperty("spring.datasource.url"));
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schema = "tc_r1_evolved_" + UUID.randomUUID().toString().replace("-", "");
        String isolatedUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }
        try {
            flyway(baseUrl, username, password, schema, "49").migrate();
            UUID predecessorId = UUID.randomUUID();
            UUID reassignedSuccessorId = UUID.randomUUID();
            UUID originalTargetId = UUID.randomUUID();
            UUID reassignmentTargetId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID authorizationId = UUID.randomUUID();
            String externalId = "evolved-reassignment-" + UUID.randomUUID();
            seedValidReassignment(isolatedUrl, username, password, predecessorId, reassignedSuccessorId,
                    originalTargetId, reassignmentTargetId, requestId, authorizationId, externalId);

            UUID transferredTargetId = UUID.randomUUID();
            UUID secondSuccessorTargetId = UUID.randomUUID();
            insertTarget(isolatedUrl, username, password, transferredTargetId);
            insertTarget(isolatedUrl, username, password, secondSuccessorTargetId);
            evolveReassignmentSuccessor(isolatedUrl, username, password, reassignedSuccessorId,
                    reassignmentTargetId, transferredTargetId);

            // The canonical R2 regression continues from REVOKE with another
            // reassignment. This leaves the first successor non-current before
            // V50, while preserving its immutable creation event.
            UUID secondSuccessorId = UUID.randomUUID();
            UUID secondRequestId = UUID.randomUUID();
            UUID secondAuthorizationId = UUID.randomUUID();
            seedFollowUpReassignment(isolatedUrl, username, password, reassignedSuccessorId,
                    secondSuccessorId, transferredTargetId, secondSuccessorTargetId,
                    secondRequestId, secondAuthorizationId, externalId);

            String originalEventBefore = jsonRow(isolatedUrl, username, password, """
                    SELECT (to_jsonb(event) - 'correction_request_id'
                            - 'target_correction_authorization_id')::text
                    FROM progression_subject_ownership_history event
                    WHERE event.event_type = 'OWNERSHIP_REASSIGNED' AND event.reassignment_request_id = ?
                    """, requestId);
            String originalAuthorizationBefore = jsonRow(isolatedUrl, username, password, """
                    SELECT to_jsonb(approval)::text FROM progression_subject_reassignment_authorization approval
                    WHERE approval.authorization_id = ?
                    """, authorizationId);
            String historyBefore = jsonRow(isolatedUrl, username, password, """
                    SELECT COALESCE(jsonb_agg(to_jsonb(history) - 'correction_request_id'
                           - 'target_correction_authorization_id' ORDER BY history.identity_id,
                           history.aggregate_version)::text, '[]')
                    FROM progression_subject_ownership_history history
                    """);
            String identitiesBefore = jsonRow(isolatedUrl, username, password, """
                    SELECT COALESCE(jsonb_agg(to_jsonb(identity) ORDER BY identity.id)::text, '[]')
                    FROM progression_subject_identity identity
                    """);
            String pointerBefore = jsonRow(isolatedUrl, username, password, """
                    SELECT to_jsonb(pointer)::text FROM progression_subject_current_binding pointer
                    WHERE pointer.namespace = 'lifeos' AND pointer.external_id = ?
                    """, externalId);
            assertThat(pointerBefore).contains(secondSuccessorId.toString());

            flyway(baseUrl, username, password, schema, "50").migrate();

            assertThat(latestVersion(isolatedUrl, username, password)).isEqualTo("50");
            assertThat(jsonRow(isolatedUrl, username, password, """
                    SELECT (to_jsonb(event) - 'correction_request_id'
                            - 'target_correction_authorization_id')::text
                    FROM progression_subject_ownership_history event
                    WHERE event.event_type = 'OWNERSHIP_REASSIGNED' AND event.reassignment_request_id = ?
                    """, requestId)).isEqualTo(originalEventBefore);
            assertThat(jsonRow(isolatedUrl, username, password, """
                    SELECT to_jsonb(approval)::text FROM progression_subject_reassignment_authorization approval
                    WHERE approval.authorization_id = ?
                    """, authorizationId)).isEqualTo(originalAuthorizationBefore);
            assertThat(jsonRow(isolatedUrl, username, password, """
                    SELECT COALESCE(jsonb_agg(to_jsonb(history) - 'correction_request_id'
                           - 'target_correction_authorization_id' ORDER BY history.identity_id,
                           history.aggregate_version)::text, '[]')
                    FROM progression_subject_ownership_history history
                    """)).isEqualTo(historyBefore);
            assertThat(jsonRow(isolatedUrl, username, password, """
                    SELECT COALESCE(jsonb_agg(to_jsonb(identity) ORDER BY identity.id)::text, '[]')
                    FROM progression_subject_identity identity
                    """)).isEqualTo(identitiesBefore);
            assertThat(jsonRow(isolatedUrl, username, password, """
                    SELECT to_jsonb(pointer)::text FROM progression_subject_current_binding pointer
                    WHERE pointer.namespace = 'lifeos' AND pointer.external_id = ?
                    """, externalId)).isEqualTo(pointerBefore);
            assertThat(latestIdentityState(isolatedUrl, username, password, reassignedSuccessorId))
                    .isEqualTo("REVOKED|UNVERIFIED|4|" + transferredTargetId);
        } finally {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }

    @Test
    void malformedV49LineageFailsV50Preflight() throws Exception {
        assertMalformedV49ReassignmentFailsV50Preflight(true);
    }

    @Test
    void malformedV49ReassignmentCorrelationFailsV50Preflight() throws Exception {
        assertMalformedV49ReassignmentFailsV50Preflight(false);
    }

    @Test
    void jpaPersistenceCanInsertAndLookupImmutableAuthorization() {
        Fixture fixture = fixture();
        UUID authorizationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        authorizationRepository.saveAndFlush(new ProgressionSubjectTargetCorrectionAuthorization(
                authorizationId, requestId, "lifeos", fixture.externalId(), fixture.predecessorId(), 0L,
                fixture.oldTarget(), fixture.newTarget(), "wrong source mapping", "source:123",
                "authority:456", "case:789", "reviewer-tcr1"));

        ProgressionSubjectTargetCorrectionAuthorization loaded = authorizationRepository
                .findByCorrectionRequestId(requestId).orElseThrow();

        assertThat(loaded.getAuthorizationId()).isEqualTo(authorizationId);
        assertThat(loaded.getCorrectionRequestId()).isEqualTo(requestId);
        assertThat(loaded.getPredecessorIdentityId()).isEqualTo(fixture.predecessorId());
        assertThat(loaded.getReviewedAt()).isNotNull();
        assertThat(loaded.getReviewedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void authorizationIsGloballyUniqueDatabaseTimedAndImmutable() {
        Fixture fixture = fixture();
        UUID authorizationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        insertAuthorization(fixture, authorizationId, requestId);

        Instant reviewedAt = jdbc.queryForObject("""
                SELECT reviewed_at FROM progression_subject_target_correction_authorization
                WHERE authorization_id = ?
                """, (rs, row) -> rs.getTimestamp(1).toInstant(), authorizationId);
        assertThat(reviewedAt).isNotNull();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_target_correction_authorization
                WHERE correction_request_id = ?
                """, Integer.class, requestId)).isEqualTo(1);

        assertThatThrownBy(() -> jdbc.update("""
                UPDATE progression_subject_target_correction_authorization
                SET correction_basis = 'tampered' WHERE authorization_id = ?
                """, authorizationId)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("""
                DELETE FROM progression_subject_target_correction_authorization WHERE authorization_id = ?
                """, authorizationId)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.execute("TRUNCATE progression_subject_target_correction_authorization"))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_target_correction_authorization
                WHERE authorization_id = ?
                """, Integer.class, authorizationId)).isEqualTo(1);
    }

    @Test
    void authorizationRejectsBlankEvidenceEqualTargetsAndDuplicateRequest() {
        Fixture fixture = fixture();
        UUID requestId = UUID.randomUUID();
        assertThatThrownBy(() -> jdbc.update(authInsertSql(), UUID.randomUUID(), requestId,
                "lifeos", fixture.externalId(), fixture.predecessorId(), 0L, fixture.oldTarget(), fixture.newTarget(),
                "basis", " ", "authoritative-fact", "case", "reviewer"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(authInsertSql(), UUID.randomUUID(), requestId,
                "lifeos", fixture.externalId(), fixture.predecessorId(), 0L, fixture.oldTarget(), fixture.oldTarget(),
                "basis", "source", "authoritative-fact", "case", "reviewer"))
                .isInstanceOf(DataAccessException.class);

        insertAuthorization(fixture, UUID.randomUUID(), requestId);
        assertThatThrownBy(() -> insertAuthorization(fixture, UUID.randomUUID(), requestId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void databaseAcceptsOnlyFullyCorrelatedCorrectionSuccessorAndPointerTransition() {
        Fixture fixture = fixture();
        UUID authorizationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID successorId = UUID.randomUUID();
        insertAuthorization(fixture, authorizationId, requestId);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("""
                    INSERT INTO progression_subject_identity
                        (id, namespace, external_id, jogador_id, identity_class, ownership_status,
                         verification_status, ownership_version, predecessor_identity_id)
                    VALUES (?, 'lifeos', ?, ?, 'EXTERNAL', 'DISABLED', 'UNVERIFIED', 0, ?)
                    """, successorId, fixture.externalId(), fixture.newTarget(), fixture.predecessorId());
            jdbc.update("""
                    INSERT INTO progression_subject_ownership_history
                        (id, identity_id, aggregate_version, event_type,
                         previous_identity_class, new_identity_class,
                         previous_target_jogador_id, new_target_jogador_id,
                         previous_ownership_status, new_ownership_status,
                         previous_verification_status, new_verification_status,
                         provenance, actor_type, actor_id, evidence_type, evidence_reference,
                         reason, effective_at, predecessor_identity_id, predecessor_ownership_version,
                         correction_request_id, target_correction_authorization_id)
                    VALUES (?, ?, 0, 'OWNERSHIP_TARGET_CORRECTED', 'EXTERNAL', 'EXTERNAL', ?, ?,
                            'ACTIVE', 'DISABLED', 'UNVERIFIED', 'UNVERIFIED', 'LOGOS_OPERATOR_ACTION',
                            'WORKLOAD_OPERATOR', 'executor-tcr1',
                            'ADMINISTRATIVE_TARGET_CORRECTION_AUTHORIZATION', ?, 'reviewed correction', ?, ?, 0, ?, ?)
                    """, UUID.randomUUID(), successorId, fixture.oldTarget(), fixture.newTarget(),
                    authorizationId.toString(), Timestamp.from(Instant.now()), fixture.predecessorId(),
                    requestId, authorizationId);
            assertThat(jdbc.update("""
                    UPDATE progression_subject_current_binding SET current_identity_id = ?
                    WHERE namespace = 'lifeos' AND external_id = ? AND current_identity_id = ?
                    """, successorId, fixture.externalId(), fixture.predecessorId())).isEqualTo(1);
        });

        assertThat(jdbc.queryForObject("""
                SELECT current_identity_id FROM progression_subject_current_binding
                WHERE namespace = 'lifeos' AND external_id = ?
                """, UUID.class, fixture.externalId())).isEqualTo(successorId);
        assertThat(jdbc.queryForObject("SELECT ownership_status FROM progression_subject_identity WHERE id = ?",
                String.class, fixture.predecessorId())).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history
                WHERE event_type = 'OWNERSHIP_TARGET_CORRECTED' AND correction_request_id = ?
                  AND target_correction_authorization_id = ?
                """, Integer.class, requestId, authorizationId)).isEqualTo(1);

        assertThatThrownBy(() -> jdbc.update("""
                UPDATE progression_subject_identity SET ownership_status = 'DISABLED'
                WHERE id = ?
                """, fixture.predecessorId())).isInstanceOf(DataAccessException.class);
    }

    @Test
    void correctionEventWithoutRequestAndAuthorizationIsRejected() {
        Fixture fixture = fixture();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO progression_subject_ownership_history
                    (id, identity_id, aggregate_version, event_type, new_identity_class,
                     new_target_jogador_id, new_ownership_status, new_verification_status, provenance)
                VALUES (?, ?, 17, 'OWNERSHIP_TARGET_CORRECTED', 'EXTERNAL', ?, 'DISABLED',
                        'UNVERIFIED', 'LOGOS_OPERATOR_ACTION')
                """, UUID.randomUUID(), fixture.predecessorId(), fixture.newTarget()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void malformedV49PointerStateFailsV50PreflightWithoutAdvancingSchema() throws Exception {
        String baseUrl = withoutCurrentSchema(environment.getRequiredProperty("spring.datasource.url"));
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schema = "logos_tc_r1_preflight_" + UUID.randomUUID().toString().replace("-", "");
        String isolatedUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }
        try {
            flyway(baseUrl, username, password, schema, "49").migrate();
            UUID user = UUID.randomUUID();
            UUID target = UUID.randomUUID();
            UUID identity = UUID.randomUUID();
            try (Connection connection = DriverManager.getConnection(isolatedUrl, username, password)) {
                insert(connection, "INSERT INTO app_user (id,email,password) VALUES (?,?,?)",
                        user, "preflight-" + user + "@example.test", "hash");
                insert(connection, "INSERT INTO jogador (id,user_id,apelido) VALUES (?,?,?)",
                        target, user, "preflight");
                insert(connection, """
                        INSERT INTO progression_subject_identity
                            (id,namespace,external_id,jogador_id,identity_class,ownership_status,
                             verification_status,ownership_version)
                        VALUES (?,'lifeos',? ,?,'EXTERNAL','ACTIVE','UNVERIFIED',0)
                        """, identity, "preflight-" + identity, target);
                try (Statement corrupt = connection.createStatement()) {
                    corrupt.execute("ALTER TABLE progression_subject_current_binding DISABLE TRIGGER ALL");
                    corrupt.execute("UPDATE progression_subject_current_binding SET current_identity_id = '"
                            + UUID.randomUUID() + "' WHERE namespace = 'lifeos'");
                    corrupt.execute("ALTER TABLE progression_subject_current_binding ENABLE TRIGGER ALL");
                }
            }
            assertThatThrownBy(() -> flyway(baseUrl, username, password, schema, "50").migrate())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("V50 preflight failed: current pointer coverage or locator is invalid");
            assertThat(latestVersion(isolatedUrl, username, password)).isEqualTo("49");
        } finally {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }

    private Fixture fixture() {
        UUID userOld = UUID.randomUUID();
        UUID userNew = UUID.randomUUID();
        UUID oldTarget = UUID.randomUUID();
        UUID newTarget = UUID.randomUUID();
        UUID predecessor = UUID.randomUUID();
        String externalId = "target-correction-" + UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id,email,password) VALUES (?,?,?)",
                userOld, userOld + "@tc-r1.test", "hash");
        jdbc.update("INSERT INTO app_user (id,email,password) VALUES (?,?,?)",
                userNew, userNew + "@tc-r1.test", "hash");
        jdbc.update("INSERT INTO jogador (id,user_id,apelido) VALUES (?,?,?)", oldTarget, userOld, "old-" + oldTarget);
        jdbc.update("INSERT INTO jogador (id,user_id,apelido) VALUES (?,?,?)", newTarget, userNew, "new-" + newTarget);
        jdbc.update("""
                INSERT INTO progression_subject_identity
                    (id,namespace,external_id,jogador_id,identity_class,ownership_status,
                     verification_status,ownership_version)
                VALUES (?,'lifeos',?,?,'EXTERNAL','ACTIVE','UNVERIFIED',0)
                """, predecessor, externalId, oldTarget);
        return new Fixture(predecessor, externalId, oldTarget, newTarget);
    }

    private void insertAuthorization(Fixture fixture, UUID authorizationId, UUID requestId) {
        jdbc.update(authInsertSql(), authorizationId, requestId, "lifeos", fixture.externalId(),
                fixture.predecessorId(), 0L, fixture.oldTarget(), fixture.newTarget(),
                "source mapping was wrong at inception", "source-record:987", "authority-record:654",
                "case:tc-r1-42", "reviewer-tcr1");
    }

    private static String authInsertSql() {
        return """
                INSERT INTO progression_subject_target_correction_authorization
                    (authorization_id,correction_request_id,namespace,external_id,
                     predecessor_identity_id,predecessor_ownership_version,
                     predecessor_target_jogador_id,corrected_target_jogador_id,
                     correction_basis,source_assertion_reference,authoritative_fact_reference,
                     reviewed_case_reference,reviewer_principal_id)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
    }

    private static Flyway flyway(String url, String username, String password, String schema, String target) {
        return Flyway.configure().dataSource(url, username, password)
                .locations("classpath:db/migration").schemas(schema).defaultSchema(schema)
                .createSchemas(false).target(MigrationVersion.fromVersion(target)).load();
    }

    private static String latestVersion(String url, String username, String password) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT version FROM flyway_schema_history WHERE success = true
                     ORDER BY installed_rank DESC LIMIT 1
                     """)) {
            if (!result.next()) throw new IllegalStateException("Missing Flyway schema history");
            return result.getString(1);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void insert(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            statement.executeUpdate();
        }
    }

    private static void seedValidReassignment(String url, String username, String password,
            UUID predecessorId, UUID successorId, UUID oldTargetId, UUID newTargetId,
            UUID requestId, UUID authorizationId, String externalId) throws SQLException {
        UUID oldUserId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            insert(connection, "INSERT INTO app_user (id,email,password) VALUES (?,?,?)",
                    oldUserId, "old-" + oldUserId + "@tc-r1.test", "hash");
            insert(connection, "INSERT INTO app_user (id,email,password) VALUES (?,?,?)",
                    newUserId, "new-" + newUserId + "@tc-r1.test", "hash");
            insert(connection, "INSERT INTO jogador (id,user_id,apelido) VALUES (?,?,?)",
                    oldTargetId, oldUserId, "old-" + oldTargetId);
            insert(connection, "INSERT INTO jogador (id,user_id,apelido) VALUES (?,?,?)",
                    newTargetId, newUserId, "new-" + newTargetId);
            insert(connection, """
                    INSERT INTO progression_subject_identity
                        (id,namespace,external_id,jogador_id,identity_class,ownership_status,
                         verification_status,ownership_version)
                    VALUES (?,'lifeos',?,?,'EXTERNAL','REVOKED','UNVERIFIED',1)
                    """, predecessorId, externalId, oldTargetId);
            // V49 validates a newly inserted identity against its selected pointer;
            // make the root a committed historical row before adding its successor.
            connection.commit();
            insert(connection, """
                    INSERT INTO progression_subject_reassignment_authorization
                        (authorization_id,reassignment_request_id,namespace,external_id,
                         predecessor_identity_id,predecessor_ownership_version,
                         predecessor_target_jogador_id,proposed_successor_target_jogador_id,
                         recovery_basis,reviewed_case_reference,reviewer_principal_id)
                    VALUES (?,?, 'lifeos', ?, ?, 1, ?, ?, 'reviewed recovery', 'case-reference', 'reviewer')
                    """, authorizationId, requestId, externalId, predecessorId, oldTargetId, newTargetId);
            insert(connection, """
                    INSERT INTO progression_subject_identity
                        (id,namespace,external_id,jogador_id,identity_class,ownership_status,
                         verification_status,ownership_version,predecessor_identity_id)
                    VALUES (?,'lifeos',?,?,'EXTERNAL','DISABLED','UNVERIFIED',0,?)
                    """, successorId, externalId, newTargetId, predecessorId);
            insert(connection, """
                    INSERT INTO progression_subject_ownership_history
                        (id,identity_id,aggregate_version,event_type,
                         previous_identity_class,new_identity_class,
                         previous_target_jogador_id,new_target_jogador_id,
                         previous_ownership_status,new_ownership_status,
                         previous_verification_status,new_verification_status,
                         provenance,actor_type,actor_id,evidence_type,evidence_reference,
                         reason,effective_at,predecessor_identity_id,predecessor_ownership_version,
                         reassignment_request_id,reassignment_authorization_id)
                    VALUES (?, ?, 0, 'OWNERSHIP_REASSIGNED', 'EXTERNAL', 'EXTERNAL', ?, ?,
                            'REVOKED', 'DISABLED', 'UNVERIFIED', 'UNVERIFIED',
                            'LOGOS_OPERATOR_ACTION', 'WORKLOAD_OPERATOR', 'executor',
                            'ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION', ?, 'recovery',
                            clock_timestamp(), ?, 1, ?, ?)
                    """, UUID.randomUUID(), successorId, oldTargetId, newTargetId,
                    authorizationId.toString(), predecessorId, requestId, authorizationId);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE progression_subject_current_binding SET current_identity_id = ?
                    WHERE namespace = 'lifeos' AND external_id = ? AND current_identity_id = ?
                    """)) {
                statement.setObject(1, successorId);
                statement.setString(2, externalId);
                statement.setObject(3, predecessorId);
                assertThat(statement.executeUpdate()).isEqualTo(1);
            }
            connection.commit();
        }
    }

    private static void insertTarget(String url, String username, String password, UUID targetId)
            throws SQLException {
        UUID userId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            insert(connection, "INSERT INTO app_user (id,email,password) VALUES (?,?,?)",
                    userId, "target-" + userId + "@tc-r1.test", "hash");
            insert(connection, "INSERT INTO jogador (id,user_id,apelido) VALUES (?,?,?)",
                    targetId, userId, "target-" + targetId);
        }
    }

    /** Persists the same lifecycle snapshots and final row values as the canonical R2 regression. */
    private static void evolveReassignmentSuccessor(String url, String username, String password,
            UUID identityId, UUID initialTargetId, UUID transferredTargetId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            lifecycleStep(connection, identityId, 1, "OWNERSHIP_REACTIVATED", initialTargetId,
                    initialTargetId, "DISABLED", "ACTIVE", "UNVERIFIED", "UNVERIFIED",
                    "reactivate-actor", null, null, "activate successor", "ACTIVE", "UNVERIFIED", 1);
            lifecycleStep(connection, identityId, 2, "OWNERSHIP_VERIFIED", initialTargetId,
                    initialTargetId, "ACTIVE", "ACTIVE", "UNVERIFIED", "VERIFIED",
                    "verify-actor", "ADMINISTRATIVE_VERIFICATION", "verification-source",
                    "verify successor", "ACTIVE", "VERIFIED", 2);
            lifecycleStep(connection, identityId, 3, "OWNERSHIP_TRANSFERRED", initialTargetId,
                    transferredTargetId, "ACTIVE", "ACTIVE", "VERIFIED", "UNVERIFIED",
                    "transfer-actor", "BILATERAL_TRANSFER_CONSENT", "bilateral-consent",
                    "subsequent legitimate transfer", "ACTIVE", "UNVERIFIED", 3);
            lifecycleStep(connection, identityId, 4, "OWNERSHIP_REVOKED", transferredTargetId,
                    transferredTargetId, "ACTIVE", "REVOKED", "UNVERIFIED", "UNVERIFIED",
                    "revoke-actor", null, null, "revoke transferred binding", "REVOKED", "UNVERIFIED", 4);
            connection.commit();
        }
    }

    private static void reactivateAndVerifySuccessor(String url, String username, String password,
            UUID identityId, UUID targetId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            lifecycleStep(connection, identityId, 1, "OWNERSHIP_REACTIVATED", targetId,
                    targetId, "DISABLED", "ACTIVE", "UNVERIFIED", "UNVERIFIED",
                    "reactivate-actor", null, null, "activate successor", "ACTIVE", "UNVERIFIED", 1);
            lifecycleStep(connection, identityId, 2, "OWNERSHIP_VERIFIED", targetId,
                    targetId, "ACTIVE", "ACTIVE", "UNVERIFIED", "VERIFIED",
                    "verify-actor", "ADMINISTRATIVE_VERIFICATION", "verification-source",
                    "verify successor", "ACTIVE", "VERIFIED", 2);
            connection.commit();
        }
    }

    private static void lifecycleStep(Connection connection, UUID identityId, long version, String eventType,
            UUID previousTarget, UUID newTarget, String previousOwnership, String newOwnership,
            String previousVerification, String newVerification, String actor, String evidenceType,
            String evidenceReference, String reason, String resultingOwnership, String resultingVerification,
            long resultingVersion) throws SQLException {
        insert(connection, """
                INSERT INTO progression_subject_ownership_history
                    (id,identity_id,aggregate_version,event_type,
                     previous_identity_class,new_identity_class,
                     previous_target_jogador_id,new_target_jogador_id,
                     previous_ownership_status,new_ownership_status,
                     previous_verification_status,new_verification_status,
                     provenance,actor_type,actor_id,evidence_type,evidence_reference,reason,effective_at)
                VALUES (?, ?, ?, ?, 'EXTERNAL', 'EXTERNAL', ?, ?, ?, ?, ?, ?,
                        'LOGOS_OPERATOR_ACTION', 'WORKLOAD_OPERATOR', ?, ?, ?, ?, clock_timestamp())
                """, UUID.randomUUID(), identityId, version, eventType, previousTarget, newTarget,
                previousOwnership, newOwnership, previousVerification, newVerification, actor,
                evidenceType, evidenceReference, reason);
        insert(connection, """
                UPDATE progression_subject_identity
                SET jogador_id = ?, ownership_status = ?, verification_status = ?, ownership_version = ?
                WHERE id = ?
                """, newTarget, resultingOwnership, resultingVerification, resultingVersion, identityId);
    }

    private static void seedFollowUpReassignment(String url, String username, String password,
            UUID predecessorId, UUID successorId, UUID predecessorTargetId, UUID successorTargetId,
            UUID requestId, UUID authorizationId, String externalId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            insert(connection, """
                    INSERT INTO progression_subject_reassignment_authorization
                        (authorization_id,reassignment_request_id,namespace,external_id,
                         predecessor_identity_id,predecessor_ownership_version,
                         predecessor_target_jogador_id,proposed_successor_target_jogador_id,
                         recovery_basis,reviewed_case_reference,reviewer_principal_id)
                    VALUES (?, ?, 'lifeos', ?, ?, 4, ?, ?, 'second reviewed recovery',
                            'second-case', 'second-reviewer')
                    """, authorizationId, requestId, externalId, predecessorId,
                    predecessorTargetId, successorTargetId);
            insert(connection, """
                    INSERT INTO progression_subject_identity
                        (id,namespace,external_id,jogador_id,identity_class,ownership_status,
                         verification_status,ownership_version,predecessor_identity_id)
                    VALUES (?, 'lifeos', ?, ?, 'EXTERNAL', 'DISABLED', 'UNVERIFIED', 0, ?)
                    """, successorId, externalId, successorTargetId, predecessorId);
            insert(connection, """
                    INSERT INTO progression_subject_ownership_history
                        (id,identity_id,aggregate_version,event_type,
                         previous_identity_class,new_identity_class,
                         previous_target_jogador_id,new_target_jogador_id,
                         previous_ownership_status,new_ownership_status,
                         previous_verification_status,new_verification_status,
                         provenance,actor_type,actor_id,evidence_type,evidence_reference,
                         reason,effective_at,predecessor_identity_id,predecessor_ownership_version,
                         reassignment_request_id,reassignment_authorization_id)
                    VALUES (?, ?, 0, 'OWNERSHIP_REASSIGNED', 'EXTERNAL', 'EXTERNAL', ?, ?,
                            'REVOKED', 'DISABLED', 'UNVERIFIED', 'UNVERIFIED',
                            'LOGOS_OPERATOR_ACTION', 'WORKLOAD_OPERATOR', 'second-executor',
                            'ADMINISTRATIVE_REASSIGNMENT_AUTHORIZATION', ?, 'second recovery',
                            clock_timestamp(), ?, 4, ?, ?)
                    """, UUID.randomUUID(), successorId, predecessorTargetId, successorTargetId,
                    authorizationId.toString(), predecessorId, requestId, authorizationId);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE progression_subject_current_binding SET current_identity_id = ?
                    WHERE namespace = 'lifeos' AND external_id = ? AND current_identity_id = ?
                    """)) {
                statement.setObject(1, successorId);
                statement.setString(2, externalId);
                statement.setObject(3, predecessorId);
                assertThat(statement.executeUpdate()).isEqualTo(1);
            }
            connection.commit();
        }
    }

    private static String jsonRow(String url, String username, String password, String sql, Object... values) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            try (var result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("Expected query result row");
                return result.getString(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String latestIdentityState(String url, String username, String password, UUID identityId) {
        return jsonRow(url, username, password, """
                SELECT concat_ws('|', ownership_status, verification_status,
                                 ownership_version::text, jogador_id::text)
                FROM progression_subject_identity WHERE id = ?
                """, identityId);
    }

    private void assertMalformedV49ReassignmentFailsV50Preflight(boolean corruptLineage) throws Exception {
        String baseUrl = withoutCurrentSchema(environment.getRequiredProperty("spring.datasource.url"));
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schema = "logos_tc_r1_bad_v49_" + UUID.randomUUID().toString().replace("-", "");
        String isolatedUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }
        try {
            flyway(baseUrl, username, password, schema, "49").migrate();
            UUID predecessorId = UUID.randomUUID();
            UUID successorId = UUID.randomUUID();
            UUID oldTargetId = UUID.randomUUID();
            UUID newTargetId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID authorizationId = UUID.randomUUID();
            String externalId = "bad-v49-" + UUID.randomUUID();
            seedValidReassignment(isolatedUrl, username, password, predecessorId, successorId,
                    oldTargetId, newTargetId, requestId, authorizationId, externalId);

            try (Connection connection = DriverManager.getConnection(isolatedUrl, username, password);
                 Statement statement = connection.createStatement()) {
                if (corruptLineage) {
                    statement.execute("ALTER TABLE progression_subject_identity DISABLE TRIGGER ALL");
                    statement.execute("UPDATE progression_subject_identity SET predecessor_identity_id = '"
                            + UUID.randomUUID() + "' WHERE id = '" + successorId + "'");
                    statement.execute("ALTER TABLE progression_subject_identity ENABLE TRIGGER ALL");
                } else {
                    statement.execute("ALTER TABLE progression_subject_ownership_history DISABLE TRIGGER ALL");
                    statement.execute("UPDATE progression_subject_ownership_history SET reassignment_request_id = '"
                            + UUID.randomUUID() + "' WHERE event_type = 'OWNERSHIP_REASSIGNED' "
                            + "AND reassignment_authorization_id = '" + authorizationId + "'");
                    statement.execute("ALTER TABLE progression_subject_ownership_history ENABLE TRIGGER ALL");
                }
            }

            assertThatThrownBy(() -> flyway(baseUrl, username, password, schema, "50").migrate())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(corruptLineage
                            ? "V50 preflight failed: identity lineage is invalid"
                            : "V50 preflight failed: existing reassignment lineage is invalid");
            assertThat(latestVersion(isolatedUrl, username, password)).isEqualTo("49");
        } finally {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }

    private static String withoutCurrentSchema(String url) {
        int separator = url.indexOf('?');
        return separator < 0 ? url : url.substring(0, separator);
    }

    private record Fixture(UUID predecessorId, String externalId, UUID oldTarget, UUID newTarget) { }
}
