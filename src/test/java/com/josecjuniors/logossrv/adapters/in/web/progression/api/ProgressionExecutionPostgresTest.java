package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionExecutionRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.adapters.out.atividadeconfig.jpa.AtividadeConfigJpaRepository;
import com.josecjuniors.logossrv.adapters.out.estresseglobal.jpa.EstresseGlobalJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.JogadorJpaRepository;
import com.josecjuniors.logossrv.adapters.out.progression.ProgressionExternalExecutionJpaRepository;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentity;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentityJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteIdempotentExternalSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProgressionExecutionPostgresTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserJpaRepository users;
    @Autowired JogadorJpaRepository jogadores;
    @Autowired EstresseGlobalJpaRepository estresses;
    @Autowired AtividadeConfigJpaRepository configs;
    @Autowired ProgressionSubjectIdentityJpaRepository identities;
    @Autowired ProgressionExternalExecutionJpaRepository executions;
    @Autowired ProgressionConfigurationResolver resolver;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired ExecuteIdempotentExternalSubjectProgressionUseCase idempotentUseCase;

    private UUID subjectId;
    private UUID jogadorId;
    private UUID secondSubjectId;
    private UUID secondJogadorId;
    private String token;
    private String configurationKey;

    @BeforeEach
    void setUp() {
        executions.deleteAll();
        identities.deleteAll();
        estresses.deleteAll();
        jogadores.deleteAll();
        configs.deleteAll();
        users.deleteAll();

        var user = users.saveAndFlush(new AppUser(new AppUserId(), "idempotency@example.test", encoder.encode("password")));
        subjectId = user.getId().getValue();
        var player = new Jogador(JogadorId.generate(), user, "idempotent-player");
        player.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), player));
        jogadores.saveAndFlush(player);
        jogadorId = player.getId().getValue();
        var learningId = jdbc.queryForObject(
                "SELECT id FROM atributo WHERE nome = 'LEARNING'", UUID.class);
        jdbc.update("""
                INSERT INTO atributo_jogador (id, jogador_id, atributo_id, xp_total, nivel_atual)
                VALUES (?, ?, ?, 0, 1)
                """, UUID.randomUUID(), jogadorId, learningId);
        identities.saveAndFlush(ProgressionSubjectIdentity.integrationVerified(UUID.randomUUID(), "lifeos", "user-1",
                player, java.time.Instant.now(), "lifeos"));
        var config = configs.saveAndFlush(new AtividadeConfig(new AtividadeConfigId(), "Reading", "fixture"));
        configurationKey = "task041-execution-" + UUID.randomUUID();
        var definitionId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var distributionId = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, current_version_id) VALUES (?, ?, NULL)",
                definitionId, configurationKey);
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress, fact_key_generation) VALUES (?, ?, 1, 1, 0, 'SEMANTIC')",
                versionId, definitionId);
        jdbc.update("INSERT INTO progression_configuration_version_distribution(id, configuration_version_id, attribute_key, weight) VALUES (?, ?, ?, 1)",
                distributionId, versionId, learningId.toString());
        jdbc.update("INSERT INTO progression_configuration_version_xp_rule(id, distribution_id, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode) VALUES (?, ?, 'pages_read', 1, 0, NULL, 'FACT_VALUE')",
                UUID.randomUUID(), distributionId);
        jdbc.update("INSERT INTO progression_configuration_version_factor(id, configuration_version_id, factor_key, tipo_input) VALUES (?, ?, 'pages_read', 'NUMERICO')",
                UUID.randomUUID(), versionId);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?",
                versionId, definitionId);
        token = jwt.generateToken(user);
    }

    @AfterEach
    void tearDown() {
        executions.deleteAll();
        identities.deleteAll();
        jdbc.update("DELETE FROM progression_configuration_version_xp_rule WHERE distribution_id IN "
                + "(SELECT d.id FROM progression_configuration_version_distribution d "
                + "JOIN progression_configuration_version v ON v.id = d.configuration_version_id "
                + "JOIN progression_configuration_definition c ON c.id = v.definition_id "
                + "WHERE c.logical_key LIKE 'task041-execution-%')");
        jdbc.update("DELETE FROM progression_configuration_version_distribution WHERE configuration_version_id IN "
                + "(SELECT v.id FROM progression_configuration_version v "
                + "JOIN progression_configuration_definition c ON c.id = v.definition_id "
                + "WHERE c.logical_key LIKE 'task041-execution-%')");
        jdbc.update("DELETE FROM progression_configuration_version_factor WHERE configuration_version_id IN "
                + "(SELECT v.id FROM progression_configuration_version v "
                + "JOIN progression_configuration_definition c ON c.id = v.definition_id "
                + "WHERE c.logical_key LIKE 'task041-execution-%')");
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = NULL WHERE logical_key LIKE 'task041-execution-%'");
        jdbc.update("DELETE FROM progression_configuration_version WHERE definition_id IN "
                + "(SELECT id FROM progression_configuration_definition WHERE logical_key LIKE 'task041-execution-%')");
        jdbc.update("DELETE FROM progression_configuration_definition WHERE logical_key LIKE 'task041-execution-%'");
        estresses.deleteAll();
        jogadores.deleteAll();
        configs.deleteAll();
        users.deleteAll();
    }

    @Test
    void sequentialDuplicateReturnsOriginalResponseAndMutatesOnce() throws Exception {
        var request = request("LifeOS", "reading-session-1", 30);
        var first = call(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var second = call(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void exactReadAlsoProtectsStoredSubjectNamespace() throws Exception {
        identities.saveAndFlush(ProgressionSubjectIdentity.integrationVerified(UUID.randomUUID(), "noema", "foreign-user",
                jogadores.findById(new JogadorId(jogadorId)).orElseThrow(), java.time.Instant.now(), "noema"));
        idempotentUseCase.execute(
                new ProgressionExecutionIdentity("lifeos", "stored-noema"),
                new ExternalSubjectReference("noema", "foreign-user"),
                new ExternalProgressionConfigurationReference(configurationKey, null),
                new ProgressionFact(List.of(new ProgressionFact.Detail("pages_read", 1))));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/internal/v1/progression/executions")
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "stored-noema"))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifiedChallengeRecoversLegacySquattedMappingBeforeIntegrationProgression() throws Exception {
        var playerA = jogadores.findById(new JogadorId(jogadorId)).orElseThrow();
        var playerBUser = users.saveAndFlush(new AppUser(new AppUserId(), "rightful-owner@example.test", encoder.encode("password")));
        var playerB = new Jogador(JogadorId.generate(), playerBUser, "rightful-owner-player");
        playerB.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), playerB));
        jogadores.saveAndFlush(playerB);
        var playerBToken = jwt.generateToken(playerBUser);
        var legacyId = "legacy-victim-" + UUID.randomUUID();
        identities.saveAndFlush(new ProgressionSubjectIdentity(UUID.randomUUID(), "lifeos", legacyId, playerA));

        var challengeResponse = mockMvc.perform(post("/api/internal/v1/progression/subject-link-challenges")
                        .header("Authorization", "Bearer " + playerBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\"lifeos\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String challenge = objectMapper.readTree(challengeResponse).get("challengeToken").asText();

        mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\"lifeos\",\"externalId\":\"" + legacyId
                                + "\",\"challengeToken\":\"" + challenge + "\"}"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT jogador_id FROM progression_subject_identity WHERE namespace='lifeos' AND external_id=?", UUID.class, legacyId))
                .isEqualTo(playerB.getId().getValue());
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace='lifeos' AND external_id=?", String.class, legacyId))
                .isEqualTo("INTEGRATION_VERIFIED");

        var executionBody = """
                {"subject":{"namespace":"lifeos","externalId":"%s"},
                "execution":{"source":"lifeos","idempotencyKey":"recovered-%s"},
                "configuration":{"key":"%s","revision":1},
                "details":[{"factorKey":"pages_read","value":13}]}
                """.formatted(legacyId, UUID.randomUUID(), configurationKey);
        mockMvc.perform(post("/api/internal/v1/progression/executions")
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                        .contentType(MediaType.APPLICATION_JSON).content(executionBody))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id=?", Long.class, jogadorId)).isEqualTo(0L);
        assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id=?", Long.class, playerB.getId().getValue())).isEqualTo(13L);
    }

    @Test
    void conflictingDuplicateReturnsConflictWithoutSecondExecution() throws Exception {
        call(request("lifeos", "reading-session-2", 30)).andExpect(status().isOk());
        call(request("lifeos", "reading-session-2", 50)).andExpect(status().isConflict());

        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateHasOneLogicalExecution() throws Exception {
        var request = request("lifeos", "reading-session-3", 30);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var calls = List.<Callable<Integer>>of(
                    () -> directConcurrent("reading-session-3"),
                    () -> directConcurrent("reading-session-3"));
            var results = new java.util.ArrayList<Integer>();
            for (var future : pool.invokeAll(calls)) results.add(future.get());
            assertThat(results).containsOnly(200);
        } finally {
            pool.shutdownNow();
        }
        assertThat(executionCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, jogadorId))
                .isEqualTo(30L);
        assertThat(attributeXp()).isEqualTo(30L);
    }

    @Test
    void concurrentConflictingFingerprintHasOneWinnerAndStableConflict() throws Exception {
        for (int iteration = 0; iteration < 5; iteration++) {
            int run = iteration;
            var start = new CountDownLatch(1);
            var pool = Executors.newFixedThreadPool(2);
            try {
                var futures = List.of(
                        pool.submit(() -> concurrentAttempt("conflicting-session-" + run, 30, start)),
                        pool.submit(() -> concurrentAttempt("conflicting-session-" + run, 50, start)));
                start.countDown();
                var attempts = futures.stream().map(future -> get(future, 10)).toList();
                assertThat(attempts).filteredOn(ConcurrentAttempt::success).hasSize(1);
                assertThat(attempts).filteredOn(attempt -> !attempt.success())
                        .singleElement().extracting(ConcurrentAttempt::failure)
                        .isInstanceOf(com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionConflictException.class);

                long winningXp = jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, jogadorId);
                assertThat(winningXp).isIn(30L, 50L);
                assertThat(attributeXp()).isEqualTo(winningXp);
                assertThat(executionCount()).isEqualTo(1);

                int winningPages = (int) winningXp;
                int losingPages = winningPages == 30 ? 50 : 30;
                assertThatCode(() -> directConcurrent("conflicting-session-" + run, winningPages, null))
                        .doesNotThrowAnyException();
                assertThatThrownBy(() -> directConcurrent("conflicting-session-" + run, losingPages, null))
                        .hasRootCauseInstanceOf(com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionConflictException.class);
                assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, jogadorId))
                        .isEqualTo(winningXp);
                assertThat(executionCount()).isEqualTo(1);
            } finally {
                pool.shutdownNow();
                executions.deleteAll();
                jdbc.update("UPDATE jogador SET xp_total = 0, nivel_atual = 1 WHERE id = ?", jogadorId);
                jdbc.update("DELETE FROM atributo_jogador WHERE jogador_id = ?", jogadorId);
            }
        }
    }

    @Test
    void concurrentDistinctExecutionsAccumulateOnTheSameSubject() throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var calls = List.<Callable<Integer>>of(
                    () -> directFactValueConcurrent("distinct-a", 30, start),
                    () -> directFactValueConcurrent("distinct-b", 20, start));
            var futures = calls.stream().map(pool::submit).toList();
            start.countDown();
            for (var future : futures) assertThat(future.get(10, TimeUnit.SECONDS)).isEqualTo(200);
        } finally {
            pool.shutdownNow();
        }

        assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, jogadorId))
                .isEqualTo(50L);
        assertThat(attributeXp()).isEqualTo(50L);
        assertThat(executionCount()).isEqualTo(2);
    }

    @Test
    void concurrentExecutionsForDifferentSubjectsUseIndependentPlayerRows() throws Exception {
        createSecondSubject();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = List.of(
                    pool.submit(() -> directSubjectConcurrent(subjectId, "subject-a", 30, start)),
                    pool.submit(() -> directSubjectConcurrent(secondSubjectId, "subject-b", 20, start)));
            start.countDown();
            for (var future : futures) assertThat(future.get(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, jogadorId)).isEqualTo(30L);
        assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, secondJogadorId)).isEqualTo(20L);
        assertThat(executionCount()).isEqualTo(2);
    }

    @Test
    void factValueExecutionsAccumulateSequentiallyInCreationOrder() {
        directFactValue("serial-a", 30);
        directFactValue("serial-b", 20);

        assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, jogadorId))
                .isEqualTo(50L);
        assertThat(attributeXp()).isEqualTo(50L);
    }

    @Test
    void factValueExecutionsAccumulateSequentiallyInReverseOrder() {
        directFactValue("serial-b", 20);
        directFactValue("serial-a", 30);

        assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, jogadorId))
                .isEqualTo(50L);
        assertThat(attributeXp()).isEqualTo(50L);
    }

    private ProgressionExecutionRequest request(String source, String key, double pages) {
        return new ProgressionExecutionRequest(
                new ProgressionExecutionRequest.SubjectReference("lifeos", "user-1"),
                new ProgressionExecutionRequest.ExecutionIdentity(source, key),
                new ProgressionExecutionRequest.ConfigurationReference(configurationKey, null),
                List.of(new ProgressionExecutionRequest.DetailRequest("pages_read", pages)));
    }

    private org.springframework.test.web.servlet.ResultActions call(ProgressionExecutionRequest request) throws Exception {
        return mockMvc.perform(post("/api/internal/v1/progression/executions")
                .header("X-Logos-Client-Id", "lifeos")
                .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private int directConcurrent(String key) {
        return directConcurrent(key, 30, null);
    }

    private int directConcurrent(String key, double pages, CountDownLatch start) {
        try {
            if (start != null) start.await(10, TimeUnit.SECONDS);
            idempotentUseCase.execute(
                    new ProgressionExecutionIdentity("lifeos", key),
                    new ExternalSubjectReference("lifeos", "user-1"),
                    new ExternalProgressionConfigurationReference(configurationKey, null),
                    new ProgressionFact(List.of(new ProgressionFact.Detail("pages_read", pages))));
            return 200;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private ConcurrentAttempt concurrentAttempt(String key, double pages, CountDownLatch start) {
        try {
            start.await(10, TimeUnit.SECONDS);
            directConcurrent(key, pages, null);
            return new ConcurrentAttempt(true, null);
        } catch (Throwable exception) {
            return new ConcurrentAttempt(false, rootCause(exception));
        }
    }

    private boolean directSubjectConcurrent(UUID subject, String key, double pages, CountDownLatch start) {
        try {
            start.await(10, TimeUnit.SECONDS);
            idempotentUseCase.execute(
                    new ProgressionExecutionIdentity("lifeos", key),
                    new ExternalSubjectReference("lifeos", subject.equals(subjectId) ? "user-1" : "user-2"),
                    new ExternalProgressionConfigurationReference(configurationKey, null),
                    new ProgressionFact(List.of(new ProgressionFact.Detail("pages_read", pages))));
            return true;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private ConcurrentAttempt get(java.util.concurrent.Future<ConcurrentAttempt> future, int timeoutSeconds) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) current = current.getCause();
        return current;
    }

    private record ConcurrentAttempt(boolean success, Throwable failure) {}

    private void createSecondSubject() {
        var user = users.saveAndFlush(new AppUser(new AppUserId(), "idempotency-second@example.test", encoder.encode("password")));
        secondSubjectId = user.getId().getValue();
        var player = new Jogador(JogadorId.generate(), user, "idempotent-second-player");
        player.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), player));
        jogadores.saveAndFlush(player);
        secondJogadorId = player.getId().getValue();
        identities.saveAndFlush(ProgressionSubjectIdentity.integrationVerified(UUID.randomUUID(), "lifeos", "user-2",
                player, java.time.Instant.now(), "lifeos"));
    }

    private int directFactValueConcurrent(String key, double pages, CountDownLatch start) {
        try {
            start.await(10, TimeUnit.SECONDS);
            directFactValue(key, pages);
            return 200;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void directFactValue(String key, double pages) {
        idempotentUseCase.execute(
                new ProgressionExecutionIdentity("lifeos", key),
                new ExternalSubjectReference("lifeos", "user-1"),
                new ExternalProgressionConfigurationReference("reading", 2),
                new ProgressionFact(List.of(new ProgressionFact.Detail("pages_read", pages))));
    }

    private long attributeXp() {
        return jdbc.queryForObject("""
                SELECT aj.xp_total
                FROM atributo_jogador aj
                JOIN atributo a ON a.id = aj.atributo_id
                WHERE aj.jogador_id = ? AND a.nome = 'LEARNING'
                """, Long.class, jogadorId);
    }

    private int executionCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM progression_external_execution", Integer.class);
    }
}
