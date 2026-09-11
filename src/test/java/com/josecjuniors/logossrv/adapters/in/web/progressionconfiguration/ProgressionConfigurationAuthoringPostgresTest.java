package com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.progressionconfiguration.application.service.ProgressionConfigurationAuthoringService;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.exception.ProgressionConfigurationAuthoringConflictException;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.exception.ProgressionConfigurationActivationConflictException;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDefinition;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDraft;
import com.josecjuniors.logossrv.core.progression.application.port.out.ExternalProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProgressionConfigurationAuthoringPostgresTest {
    private static final UUID LEARNING_ATTRIBUTE = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private FatorCalculoRepository factors;
    @Autowired private ProgressionConfigurationAuthoringService service;
    @Autowired private ExternalProgressionConfigurationResolver externalResolver;
    @Autowired private AppUserJpaRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        AppUser user = users.save(new AppUser(new AppUserId(), "authoring-" + UUID.randomUUID() + "@test.local", passwordEncoder.encode("password")));
        token = jwtService.generateToken(user);
    }

    @Test
    void createsIndependentDefinitionAndEmptyDraftWithoutRuntimeVersion() throws Exception {
        String key = "piano_minutes_" + UUID.randomUUID().toString().replace('-', '_');

        mockMvc.perform(post("/api/progression/configurations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("logicalKey", " " + key.toUpperCase() + " "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logicalKey").value(key))
                .andExpect(jsonPath("$.currentRevision").value(nullValue()))
                .andExpect(jsonPath("$.activationVersion").value(0))
                .andExpect(jsonPath("$.draftVersion").value(0));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_definition WHERE logical_key = ? AND legacy_atividade_config_id IS NULL AND current_version_id IS NULL", Integer.class, key)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_draft d JOIN progression_configuration_definition c ON c.id = d.definition_id WHERE c.logical_key = ?", Integer.class, key)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_version v JOIN progression_configuration_definition c ON c.id = v.definition_id WHERE c.logical_key = ?", Integer.class, key)).isEqualTo(0);
    }

    @Test
    void duplicateNormalizedLogicalKeyReturnsConflict() throws Exception {
        String key = "focus_" + UUID.randomUUID().toString().replace('-', '_');
        service.create(key);

        mockMvc.perform(post("/api/progression/configurations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logicalKey\":\" " + key.toUpperCase() + " \"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void readsAndReplacesDynamicDraftWithOptimisticConcurrency() throws Exception {
        String key = "piano_" + UUID.randomUUID().toString().replace('-', '_');
        service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Piano " + UUID.randomUUID(), "min", TipoInput.NUMERICO, "piano_minutes_" + UUID.randomUUID().toString().replace('-', '_')));
        String fact = factor.getSemanticKey();
        String body = """
                {"expectedVersion":0,"baseXp":10,"baseStress":1,
                 "factors":["%s"],"distributions":[{"attributeId":"%s","weight":1.0,
                 "xpRules":[{"fact":"%s","multiplier":1.0,"minCutoff":null,"maxCutoff":null,"calculationMode":"FACT_VALUE"}],"stressRules":[]}]}
                """.formatted(fact, LEARNING_ATTRIBUTE, fact);

        mockMvc.perform(get("/api/progression/configurations/{key}", key).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.logicalKey").value(key)).andExpect(jsonPath("$.draftVersion").value(0));
        mockMvc.perform(get("/api/progression/configurations/{key}/draft", key).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.factors").isEmpty());
        mockMvc.perform(put("/api/progression/configurations/{key}/draft", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.factors[0]").value(fact));

        mockMvc.perform(put("/api/progression/configurations/{key}/draft", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT version FROM progression_configuration_draft d JOIN progression_configuration_definition c ON c.id = d.definition_id WHERE c.logical_key = ?", Long.class, key)).isEqualTo(1L);
    }

    @Test
    void unknownFactIsRejectedAndLegacyDefinitionIsProtected() throws Exception {
        String key = "unknown_fact_" + UUID.randomUUID().toString().replace('-', '_');
        service.create(key);
        String legacyKey = "legacy_fixture_" + UUID.randomUUID().toString().replace('-', '_');
        UUID activityId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();
        jdbc.update("INSERT INTO atividade_config(id, nome) VALUES (?, ?)", activityId, "Legacy fixture " + activityId);
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, legacy_atividade_config_id, current_version_id) VALUES (?, ?, ?, NULL)", definitionId, legacyKey, activityId);
        String body = "{\"expectedVersion\":0,\"factors\":[\"never_registered\"],\"distributions\":[]}";
        mockMvc.perform(put("/api/progression/configurations/{key}/draft", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/progression/configurations/{key}/draft", legacyKey).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void publishesSemanticSnapshotWithoutActivatingAndKeepsItImmutable() throws Exception {
        String key = "publish_" + UUID.randomUUID().toString().replace('-', '_');
        service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Publish factor " + UUID.randomUUID(), "min",
                TipoInput.NUMERICO, "piano_minutes_" + UUID.randomUUID().toString().replace('-', '_')));
        String fact = factor.getSemanticKey();
        replaceDraft(key, 0, fact, 10, 1);

        String publish = mockMvc.perform(post("/api/progression/configurations/{key}/publish", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedDraftVersion\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1))
                .andReturn().getResponse().getContentAsString();
        UUID version = UUID.fromString(objectMapper.readTree(publish).get("versionId").asText());

        assertThat(jdbc.queryForObject("SELECT current_version_id FROM progression_configuration_definition WHERE logical_key = ?", UUID.class, key)).isNull();
        assertThat(jdbc.queryForObject("SELECT fact_key_generation FROM progression_configuration_version WHERE id = ?", String.class, version)).isEqualTo("SEMANTIC");
        assertThat(jdbc.queryForObject("SELECT factor_key FROM progression_configuration_version_factor WHERE configuration_version_id = ?", String.class, version)).isEqualTo(fact);
        assertThat(jdbc.queryForObject("SELECT factor_key FROM progression_configuration_version_xp_rule r JOIN progression_configuration_version_distribution d ON d.id = r.distribution_id WHERE d.configuration_version_id = ?", String.class, version)).isEqualTo(fact);

        String retry = publish(key, 1);
        assertThat(objectMapper.readTree(retry).get("versionId").asText()).isEqualTo(version.toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_version WHERE definition_id = (SELECT id FROM progression_configuration_definition WHERE logical_key = ?)", Integer.class, key)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT activation_version FROM progression_configuration_definition WHERE logical_key = ?", Long.class, key)).isZero();

        replaceDraft(key, 1, fact, 20, 2);
        String lateRetry = publish(key, 1);
        assertThat(objectMapper.readTree(lateRetry).get("versionId").asText()).isEqualTo(version.toString());
        assertThat(jdbc.queryForObject("SELECT base_xp FROM progression_configuration_version WHERE id = ?", Integer.class, version)).isEqualTo(10);
        publish(key, 2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_version WHERE definition_id = (SELECT id FROM progression_configuration_definition WHERE logical_key = ?)", Integer.class, key)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT base_xp FROM progression_configuration_version WHERE id = ?", Integer.class, version)).isEqualTo(10);
    }

    @Test
    void staleUnpublishedDraftVersionReturnsConflict() throws Exception {
        String key = "stale_publish_" + UUID.randomUUID().toString().replace('-', '_');
        service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Stale publish factor " + UUID.randomUUID(), "min",
                TipoInput.NUMERICO, "stale_minutes_" + UUID.randomUUID().toString().replace('-', '_')));
        entityManager.flush();
        replaceDraft(key, 0, factor.getSemanticKey(), 10, 1);

        mockMvc.perform(post("/api/progression/configurations/{key}/publish", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedDraftVersion\":0}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_version WHERE definition_id = (SELECT id FROM progression_configuration_definition WHERE logical_key = ?)", Integer.class, key)).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSameDraftPublicationReturnsOneImmutableVersion() throws Exception {
        String key = "concurrent_publish_" + UUID.randomUUID().toString().replace('-', '_');
        var definition = service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Concurrent publish factor " + UUID.randomUUID(), "min",
                TipoInput.NUMERICO, "concurrent_minutes_" + UUID.randomUUID().toString().replace('-', '_')));
        replaceDraft(key, 0, factor.getSemanticKey(), 10, 1);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> { start.await(); return service.publish(key, 1); });
            var second = executor.submit(() -> { start.await(); return service.publish(key, 1); });
            start.countDown();
            var publishedFirst = first.get();
            var publishedSecond = second.get();
            assertThat(publishedFirst.versionId()).isEqualTo(publishedSecond.versionId());
            assertThat(publishedFirst.revision()).isEqualTo(publishedSecond.revision());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_version WHERE definition_id = ?", Integer.class, definition.id())).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            jdbc.update("DELETE FROM progression_configuration_version_xp_rule WHERE distribution_id IN (SELECT id FROM progression_configuration_version_distribution WHERE configuration_version_id IN (SELECT id FROM progression_configuration_version WHERE definition_id = ?))", definition.id());
            jdbc.update("DELETE FROM progression_configuration_version_stress_rule WHERE distribution_id IN (SELECT id FROM progression_configuration_version_distribution WHERE configuration_version_id IN (SELECT id FROM progression_configuration_version WHERE definition_id = ?))", definition.id());
            jdbc.update("DELETE FROM progression_configuration_version_distribution WHERE configuration_version_id IN (SELECT id FROM progression_configuration_version WHERE definition_id = ?)", definition.id());
            jdbc.update("DELETE FROM progression_configuration_version_factor WHERE configuration_version_id IN (SELECT id FROM progression_configuration_version WHERE definition_id = ?)", definition.id());
            jdbc.update("DELETE FROM progression_configuration_version WHERE definition_id = ?", definition.id());
            jdbc.update("DELETE FROM progression_configuration_draft WHERE definition_id = ?", definition.id());
            jdbc.update("DELETE FROM progression_configuration_definition WHERE id = ?", definition.id());
            jdbc.update("DELETE FROM fator_calculo WHERE id = ?", factor.getId().getValue());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentPublicationAndReplacementCannotCreateHybridSnapshot() throws Exception {
        String key = "publish_replace_" + UUID.randomUUID().toString().replace('-', '_');
        var definition = service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Publish replace factor " + UUID.randomUUID(), "min",
                TipoInput.NUMERICO, "replace_minutes_" + UUID.randomUUID().toString().replace('-', '_')));
        replaceDraftWithoutHttp(key, 0, factor.getSemanticKey(), 10, 1);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var publication = executor.submit(() -> { start.await(); return service.publish(key, 1); });
            var replacement = executor.submit(() -> {
                start.await();
                return service.replaceDraft(key, 1, new ProgressionConfigurationDraft(definition.id(), 1, 20, 2,
                        java.util.List.of(factor.getSemanticKey()), java.util.List.of()));
            });
            start.countDown();
            com.josecjuniors.logossrv.core.progressionconfiguration.domain.repository.ProgressionConfigurationAuthoringRepository.PublishedProgressionConfigurationVersion published = null;
            Throwable publicationFailure = null;
            try {
                published = publication.get();
            } catch (ExecutionException exception) {
                publicationFailure = exception.getCause();
            }
            Throwable replacementFailure = null;
            try {
                replacement.get();
            } catch (ExecutionException exception) {
                replacementFailure = exception.getCause();
            }
            if (published != null) {
                assertThat(jdbc.queryForObject("SELECT base_xp FROM progression_configuration_version WHERE id = ?", Integer.class, published.versionId())).isEqualTo(10);
                assertThat(jdbc.queryForObject("SELECT factor_key FROM progression_configuration_version_factor WHERE configuration_version_id = ?", String.class, published.versionId())).isEqualTo(factor.getSemanticKey());
                if (replacementFailure != null) {
                    assertThat(replacementFailure).isInstanceOf(com.josecjuniors.logossrv.core.progressionconfiguration.domain.exception.ProgressionConfigurationAuthoringConflictException.class);
                }
            } else {
                assertThat(publicationFailure).isInstanceOf(com.josecjuniors.logossrv.core.progressionconfiguration.domain.exception.ProgressionConfigurationAuthoringConflictException.class);
                assertThat(replacementFailure).isNull();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_version WHERE definition_id = ?", Integer.class, definition.id())).isZero();
            }
        } finally {
            executor.shutdownNow();
            jdbc.update("DELETE FROM progression_configuration_version_factor WHERE configuration_version_id IN (SELECT id FROM progression_configuration_version WHERE definition_id = ?)", definition.id());
            jdbc.update("DELETE FROM progression_configuration_version WHERE definition_id = ?", definition.id());
            jdbc.update("DELETE FROM progression_configuration_draft WHERE definition_id = ?", definition.id());
            jdbc.update("DELETE FROM progression_configuration_definition WHERE id = ?", definition.id());
            jdbc.update("DELETE FROM fator_calculo WHERE id = ?", factor.getId().getValue());
        }
    }

    @Test
    void rejectsIncompletePublicationAndDoesNotCreateRuntimeVersion() throws Exception {
        String key = "incomplete_publish_" + UUID.randomUUID().toString().replace('-', '_');
        service.create(key);

        mockMvc.perform(post("/api/progression/configurations/{key}/publish", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedDraftVersion\":0}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_version WHERE definition_id = (SELECT id FROM progression_configuration_definition WHERE logical_key = ?)", Integer.class, key)).isZero();
    }

    @Test
    void rejectsLegacyFactWithoutSemanticIdentityAtPublicationBoundary() throws Exception {
        String key = "legacy_fact_publish_" + UUID.randomUUID().toString().replace('-', '_');
        var definition = service.create(key);
        FatorCalculo legacy = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Legacy publish factor " + UUID.randomUUID(), "min", TipoInput.NUMERICO));
        entityManager.flush();
        UUID draftId = jdbc.queryForObject("SELECT id FROM progression_configuration_draft WHERE definition_id = ?", UUID.class, definition.id());
        jdbc.update("INSERT INTO progression_configuration_draft_factor(id, draft_id, fator_calculo_id) VALUES (?, ?, ?)", UUID.randomUUID(), draftId, legacy.getId().getValue());
        jdbc.update("UPDATE progression_configuration_draft SET base_xp = 10, base_stress = 1 WHERE id = ?", draftId);

        mockMvc.perform(post("/api/progression/configurations/{key}/publish", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedDraftVersion\":0}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_configuration_version WHERE definition_id = ?", Integer.class, definition.id())).isZero();
    }

    @Test
    void unknownDefinitionCannotBePublished() throws Exception {
        mockMvc.perform(post("/api/progression/configurations/{key}/publish", "missing_" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedDraftVersion\":0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void activatesRollsBackDeactivatesAndKeepsHistoricalResolution() throws Exception {
        String key = "activation_" + UUID.randomUUID().toString().replace('-', '_');
        service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Activation factor " + UUID.randomUUID(), "min",
                TipoInput.NUMERICO, "activation_minutes_" + UUID.randomUUID().toString().replace('-', '_')));
        replaceDraft(key, 0, factor.getSemanticKey(), 10, 1);
        publish(key, 1);

        mockMvc.perform(post("/api/progression/configurations/{key}/versions/1/activate", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentRevision").value(1))
                .andExpect(jsonPath("$.activationVersion").value(1));

        mockMvc.perform(post("/api/progression/configurations/{key}/versions/1/activate", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activationVersion").value(1));

        replaceDraft(key, 1, factor.getSemanticKey(), 20, 2);
        publish(key, 2);
        mockMvc.perform(post("/api/progression/configurations/{key}/versions/2/activate", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentRevision").value(2))
                .andExpect(jsonPath("$.activationVersion").value(2));

        mockMvc.perform(post("/api/progression/configurations/{key}/versions/1/activate", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentRevision").value(1))
                .andExpect(jsonPath("$.activationVersion").value(3));

        mockMvc.perform(post("/api/progression/configurations/{key}/deactivate", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentRevision").value(nullValue()))
                .andExpect(jsonPath("$.activationVersion").value(4));

        assertThat(externalResolver.resolve(new ExternalProgressionConfigurationReference(key, null))).isEmpty();
        assertThat(externalResolver.resolve(new ExternalProgressionConfigurationReference(key, 1))).isPresent();

        mockMvc.perform(post("/api/progression/configurations/{key}/deactivate", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activationVersion").value(4));
    }

    @Test
    void rejectsUnknownRevisionStaleMutationAndLegacyLinkedActivation() throws Exception {
        String key = "activation_conflict_" + UUID.randomUUID().toString().replace('-', '_');
        service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Activation conflict factor " + UUID.randomUUID(), "min",
                TipoInput.NUMERICO, "activation_conflict_minutes_" + UUID.randomUUID().toString().replace('-', '_')));
        replaceDraft(key, 0, factor.getSemanticKey(), 10, 1);
        publish(key, 1);

        mockMvc.perform(post("/api/progression/configurations/{key}/versions/99/activate", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":0}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/progression/configurations/{key}/versions/1/activate", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":1}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT current_version_id FROM progression_configuration_definition WHERE logical_key = ?", UUID.class, key)).isNull();

        String legacyKey = "legacy_activation_" + UUID.randomUUID().toString().replace('-', '_');
        UUID activityId = UUID.randomUUID();
        jdbc.update("INSERT INTO atividade_config(id, nome) VALUES (?, ?)", activityId, "Legacy activation " + activityId);
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, legacy_atividade_config_id, current_version_id) VALUES (?, ?, ?, NULL)",
                UUID.randomUUID(), legacyKey, activityId);
        mockMvc.perform(post("/api/progression/configurations/{key}/versions/1/activate", legacyKey)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":0}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/progression/configurations/{key}/deactivate", legacyKey)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedActivationVersion\":0}"))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentDifferentTargetActivationHasOneWinner() throws Exception {
        String key = "activation_race_" + UUID.randomUUID().toString().replace('-', '_');
        var definition = service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Activation race factor " + UUID.randomUUID(), "min",
                TipoInput.NUMERICO, "activation_race_minutes_" + UUID.randomUUID().toString().replace('-', '_')));
        replaceDraftWithoutHttp(key, 0, factor.getSemanticKey(), 10, 1);
        publish(key, 1);
        replaceDraftWithoutHttp(key, 1, factor.getSemanticKey(), 20, 2);
        publish(key, 2);

        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> after(start, () -> service.activate(key, 1, 0)));
            var second = executor.submit(() -> after(start, () -> service.activate(key, 2, 0)));
            start.countDown();
            var firstResult = first.get();
            var secondResult = second.get();
            assertThat(java.util.stream.Stream.of(firstResult, secondResult).filter(ActivationAttempt::succeeded).count()).isEqualTo(1);
            assertThat(java.util.stream.Stream.of(firstResult, secondResult).filter(result -> !result.succeeded()).findFirst().orElseThrow().failure())
                    .isInstanceOf(ProgressionConfigurationActivationConflictException.class);
            assertThat(jdbc.queryForObject("SELECT activation_version FROM progression_configuration_definition WHERE id = ?", Long.class, definition.id())).isEqualTo(1L);
            assertThat(jdbc.queryForObject("SELECT v.revision FROM progression_configuration_definition d JOIN progression_configuration_version v ON v.id = d.current_version_id WHERE d.id = ?", Integer.class, definition.id()))
                    .isIn(1, 2);
        } finally {
            executor.shutdownNow();
            cleanupDefinition(definition.id(), factor.getId().getValue());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSameTargetActivationIncrementsOnlyOnce() throws Exception {
        String key = "activation_same_target_" + UUID.randomUUID().toString().replace('-', '_');
        var definition = service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Same target factor " + UUID.randomUUID(), "min",
                TipoInput.NUMERICO, "same_target_minutes_" + UUID.randomUUID().toString().replace('-', '_')));
        replaceDraftWithoutHttp(key, 0, factor.getSemanticKey(), 10, 1);
        publish(key, 1);

        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> after(start, () -> service.activate(key, 1, 0)));
            var second = executor.submit(() -> after(start, () -> service.activate(key, 1, 0)));
            start.countDown();
            assertThat(first.get().succeeded()).isTrue();
            assertThat(second.get().succeeded()).isTrue();
            assertThat(jdbc.queryForObject("SELECT activation_version FROM progression_configuration_definition WHERE id = ?", Long.class, definition.id())).isEqualTo(1L);
            assertThat(jdbc.queryForObject("SELECT v.revision FROM progression_configuration_definition d JOIN progression_configuration_version v ON v.id = d.current_version_id WHERE d.id = ?", Integer.class, definition.id())).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            cleanupDefinition(definition.id(), factor.getId().getValue());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentActivationAndDeactivationHasOneWinner() throws Exception {
        String key = "activation_deactivation_race_" + UUID.randomUUID().toString().replace('-', '_');
        var definition = service.create(key);
        FatorCalculo factor = factors.save(new FatorCalculo(FatorCalculoId.generate(), "Activation deactivation factor " + UUID.randomUUID(), "min",
                TipoInput.NUMERICO, "act_deact_min_" + UUID.randomUUID().toString().replace('-', '_')));
        replaceDraftWithoutHttp(key, 0, factor.getSemanticKey(), 10, 1);
        publish(key, 1);
        replaceDraftWithoutHttp(key, 1, factor.getSemanticKey(), 20, 2);
        publish(key, 2);
        service.activate(key, 1, 0);

        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var activation = executor.submit(() -> after(start, () -> service.activate(key, 2, 1)));
            var deactivation = executor.submit(() -> after(start, () -> service.deactivate(key, 1)));
            start.countDown();
            var activationResult = activation.get();
            var deactivationResult = deactivation.get();
            assertThat(java.util.stream.Stream.of(activationResult, deactivationResult).filter(ActivationAttempt::succeeded).count()).isEqualTo(1);
            assertThat(java.util.stream.Stream.of(activationResult, deactivationResult).filter(result -> !result.succeeded()).findFirst().orElseThrow().failure())
                    .isInstanceOf(ProgressionConfigurationActivationConflictException.class);
            assertThat(jdbc.queryForObject("SELECT activation_version FROM progression_configuration_definition WHERE id = ?", Long.class, definition.id())).isEqualTo(2L);
        } finally {
            executor.shutdownNow();
            cleanupDefinition(definition.id(), factor.getId().getValue());
        }
    }

    private void replaceDraft(String key, long expectedVersion, String fact, int baseXp, int baseStress) throws Exception {
        String body = """
                {"expectedVersion":%d,"baseXp":%d,"baseStress":%d,
                 "factors":["%s"],"distributions":[{"attributeId":"%s","weight":1.0,
                 "xpRules":[{"fact":"%s","multiplier":1.0,"minCutoff":null,"maxCutoff":null,"calculationMode":"FACT_VALUE"}],"stressRules":[]}]}
                """.formatted(expectedVersion, baseXp, baseStress, fact, LEARNING_ATTRIBUTE, fact);
        mockMvc.perform(put("/api/progression/configurations/{key}/draft", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private String publish(String key, long expectedVersion) throws Exception {
        return mockMvc.perform(post("/api/progression/configurations/{key}/publish", key)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedDraftVersion\":" + expectedVersion + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void replaceDraftWithoutHttp(String key, long expectedVersion, String fact, int baseXp, int baseStress) {
        service.replaceDraft(key, expectedVersion, new ProgressionConfigurationDraft(
                service.get(key).id(), expectedVersion, baseXp, baseStress,
                java.util.List.of(fact), java.util.List.of()));
    }

    private ActivationAttempt after(CountDownLatch start, Callable<ProgressionConfigurationDefinition> action) {
        try {
            start.await();
            return new ActivationAttempt(action.call(), null);
        } catch (Throwable failure) {
            return new ActivationAttempt(null, failure);
        }
    }

    private void cleanupDefinition(UUID definitionId, UUID factorId) {
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = NULL WHERE id = ?", definitionId);
        jdbc.update("DELETE FROM progression_configuration_version_xp_rule WHERE distribution_id IN (SELECT id FROM progression_configuration_version_distribution WHERE configuration_version_id IN (SELECT id FROM progression_configuration_version WHERE definition_id = ?))", definitionId);
        jdbc.update("DELETE FROM progression_configuration_version_stress_rule WHERE distribution_id IN (SELECT id FROM progression_configuration_version_distribution WHERE configuration_version_id IN (SELECT id FROM progression_configuration_version WHERE definition_id = ?))", definitionId);
        jdbc.update("DELETE FROM progression_configuration_version_distribution WHERE configuration_version_id IN (SELECT id FROM progression_configuration_version WHERE definition_id = ?)", definitionId);
        jdbc.update("DELETE FROM progression_configuration_version_factor WHERE configuration_version_id IN (SELECT id FROM progression_configuration_version WHERE definition_id = ?)", definitionId);
        jdbc.update("DELETE FROM progression_configuration_version WHERE definition_id = ?", definitionId);
        jdbc.update("DELETE FROM progression_configuration_draft_factor WHERE draft_id IN (SELECT id FROM progression_configuration_draft WHERE definition_id = ?)", definitionId);
        jdbc.update("DELETE FROM progression_configuration_draft_distribution WHERE draft_id IN (SELECT id FROM progression_configuration_draft WHERE definition_id = ?)", definitionId);
        jdbc.update("DELETE FROM progression_configuration_draft WHERE definition_id = ?", definitionId);
        jdbc.update("DELETE FROM progression_configuration_definition WHERE id = ?", definitionId);
        jdbc.update("DELETE FROM fator_calculo WHERE id = ?", factorId);
    }

    private record ActivationAttempt(ProgressionConfigurationDefinition state, Throwable failure) {
        boolean succeeded() { return state != null; }
    }
}
