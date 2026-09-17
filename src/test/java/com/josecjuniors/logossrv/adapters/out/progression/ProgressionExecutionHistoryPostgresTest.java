package com.josecjuniors.logossrv.adapters.out.progression;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPageRequest;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionStatus;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProgressionExecutionHistoryPostgresTest {
    @Autowired ProgressionExternalExecutionJpaRepository repository;
    @Autowired JpaProgressionExecutionHistoryReadAdapter history;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EntityManager entityManager;

    private final List<UUID> ownedIds = new ArrayList<>();
    private UUID configurationVersionId;
    private UUID skillPolicyVersionId;

    @BeforeEach
    void setUp() {
        configurationVersionId = jdbc.queryForObject(
                "select id from progression_configuration_version limit 1", UUID.class);
        skillPolicyVersionId = jdbc.queryForObject(
                "select id from progression_skill_policy_version limit 1", UUID.class);
    }

    @AfterEach
    void cleanUp() {
        for (var id : ownedIds) {
            jdbc.update("delete from progression_external_execution where id = ?", id);
        }
    }

    @Test
    void returnsCrossSourceHistoryForOnlyTheNormalizedSubject() throws Exception {
        var subject = new ExternalSubjectReference(" LIFEOS ", "subject-039");
        save("source-a", "first", subject, "COMPLETED", validOutcomeJson());
        save("source-b", "second", subject, "FAILED", "not-an-outcome");
        save("source-a", "other-namespace", new ExternalSubjectReference("other", "subject-039"),
                "FAILED", "not-an-outcome");
        save("source-a", "other-subject", new ExternalSubjectReference("lifeos", "other"),
                "FAILED", "not-an-outcome");

        var result = history.find(subject, new ProgressionExecutionHistoryPageRequest(0, 20));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.items()).extracting(item -> item.execution().identity().idempotencyKey())
                .containsExactlyInAnyOrder("first", "second");
        assertThat(result.items()).filteredOn(item -> item.execution().identity().idempotencyKey().equals("second"))
                .singleElement().extracting(item -> item.execution().outcome()).isNull();
    }

    @Test
    void ordersCanonicalRowsBeforeLegacyRowsAndPaginates() throws Exception {
        var subject = new ExternalSubjectReference("lifeos", "ordered-039");
        var canonicalOld = save("order", "old", subject, "FAILED", "not-an-outcome");
        var canonicalNew = save("order", "new", subject, "FAILED", "not-an-outcome");
        insertLegacy("order", "legacy", subject, LocalDateTime.of(2026, 1, 1, 10, 0));

        jdbc.update("update progression_external_execution set occurred_at = ? where id = ?",
                Timestamp.from(Instant.parse("2026-01-02T10:00:00Z")), canonicalOld);
        jdbc.update("update progression_external_execution set occurred_at = ? where id = ?",
                Timestamp.from(Instant.parse("2026-01-03T10:00:00Z")), canonicalNew);

        var first = history.find(subject, new ProgressionExecutionHistoryPageRequest(0, 2));
        var second = history.find(subject, new ProgressionExecutionHistoryPageRequest(1, 2));

        assertThat(first.items()).extracting(item -> item.execution().identity().idempotencyKey())
                .containsExactly("new", "old");
        assertThat(first.items()).allMatch(item -> item.occurredAt() != null);
        assertThat(second.items()).extracting(item -> item.execution().identity().idempotencyKey())
                .containsExactly("legacy");
        assertThat(second.items().get(0).occurredAt()).isNull();
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    void rejectsNoMoreThanOneHundredItemsAtApplicationBoundary() {
        assertThat(new ProgressionExecutionHistoryPageRequest(0, 100).size()).isEqualTo(100);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new ProgressionExecutionHistoryPageRequest(0, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void databaseHasCanonicalOccurredAtTypeDefaultAndHistoryIndex() {
        var column = jdbc.queryForMap("""
                select data_type, is_nullable, column_default
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'progression_external_execution'
                  and column_name = 'occurred_at'
                """);
        assertThat(column.get("data_type")).isEqualTo("timestamp with time zone");
        assertThat(column.get("is_nullable")).isEqualTo("YES");
        assertThat(column.get("column_default").toString()).contains("CURRENT_TIMESTAMP");

        assertThat(jdbc.queryForObject("""
                select count(*) from pg_indexes
                where schemaname = current_schema()
                  and tablename = 'progression_external_execution'
                  and indexname = 'ix_progression_external_execution_subject_history'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void legacyRowIsNullableAndNewRowsReceiveDatabaseInstant() throws Exception {
        var subject = new ExternalSubjectReference("lifeos", "temporal-039");
        var legacy = insertLegacy("temporal", "legacy", subject, LocalDateTime.of(2026, 1, 1, 10, 0));
        var current = save("temporal", "current", subject, "FAILED", "not-an-outcome");

        assertThat(jdbc.queryForObject("select occurred_at from progression_external_execution where id = ?",
                Instant.class, legacy)).isNull();
        assertThat(jdbc.queryForObject("select occurred_at from progression_external_execution where id = ?",
                Instant.class, current)).isNotNull();
        assertThat(history.find(subject, new ProgressionExecutionHistoryPageRequest(0, 20)).items())
                .filteredOn(item -> item.execution().identity().idempotencyKey().equals("legacy"))
                .singleElement().extracting(item -> item.occurredAt()).isNull();
    }

    @Test
    void databaseGeneratedOccurrenceSurvivesRetryAndCompletion() throws Exception {
        var subject = new ExternalSubjectReference("lifeos", "immutable-039");
        var id = save("immutable", "execution", subject, "PENDING", "not-an-outcome");
        var original = jdbc.queryForObject("select occurred_at from progression_external_execution where id = ?",
                Instant.class, id);

        var entity = repository.findById(id).orElseThrow();
        assertThat(entity.getOccurredAt()).isEqualTo(original);
        entityManager.clear();
        assertThat(repository.findById(id).orElseThrow().getOccurredAt()).isEqualTo(original);

        entity = repository.findById(id).orElseThrow();
        entity.markAttempt("PROCESSING", "temporary");
        repository.saveAndFlush(entity);
        entityManager.clear();
        assertThat(repository.findById(id).orElseThrow().getOccurredAt()).isEqualTo(original);

        entity = repository.findById(id).orElseThrow();
        entity.markCompleted(validOutcomeJson());
        repository.saveAndFlush(entity);
        entityManager.clear();

        assertThat(jdbc.queryForObject("select occurred_at from progression_external_execution where id = ?",
                Instant.class, id)).isEqualTo(original);
        assertThat(repository.findById(id).orElseThrow().getOccurredAt()).isEqualTo(original);
    }

    private UUID save(String source, String key, ExternalSubjectReference subject,
                      String status, String response) {
        var id = UUID.randomUUID();
        ownedIds.add(id);
        repository.saveAndFlush(new ProgressionExternalExecutionEntity(id, source, key,
                "fingerprint-" + id, response, "{}", status, 1, null,
                subject.namespace(), subject.externalId(), "reading", 1,
                configurationVersionId, skillPolicyVersionId));
        return id;
    }

    private UUID insertLegacy(String source, String key, ExternalSubjectReference subject,
                              LocalDateTime createdAt) {
        var id = UUID.randomUUID();
        ownedIds.add(id);
        jdbc.update("""
                insert into progression_external_execution
                (id, source_system, idempotency_key, request_fingerprint, subject_namespace,
                 subject_external_id, configuration_key, requested_revision,
                 configuration_version_id, skill_policy_version_id, response_json,
                 created_at, request_json, processing_status, attempt_count, last_error, occurred_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '{}', 'FAILED', 1, null, null)
                """, id, source, key, "fingerprint-" + id, subject.namespace(), subject.externalId(),
                "reading", 1, configurationVersionId, skillPolicyVersionId, "not-an-outcome",
                Timestamp.valueOf(createdAt));
        return id;
    }

    private String validOutcomeJson() throws Exception {
        return objectMapper.writeValueAsString(new ProgressionOutcome(
                new ProgressionResult(1, 1, List.of()),
                new ProgressionProfile(1, 1, 0, 0, List.of(), List.of())));
    }
}
