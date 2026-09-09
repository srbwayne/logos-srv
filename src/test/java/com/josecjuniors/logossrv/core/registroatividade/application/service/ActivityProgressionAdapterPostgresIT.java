package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.adapters.out.atributo.jpa.AtributoJpaRepository;
import com.josecjuniors.logossrv.adapters.out.progression.ProgressionExternalExecutionJpaRepository;
import com.josecjuniors.logossrv.adapters.out.registroatividade.jpa.RegistroAtividadeJpaRepository;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;
import com.josecjuniors.logossrv.core.progression.application.port.out.ActivityProgressionExecutionStore;
import com.josecjuniors.logossrv.core.progression.application.port.out.VersionedProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.application.service.ConfiguredStatefulProgressionApplicationService;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionExecutionFingerprint;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeCommand;
import com.josecjuniors.logossrv.adapters.in.web.registroatividade.dto.request.DetalheRegistroRequest;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionConflictException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.XpCalculationMode;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ActivityProgressionAdapterPostgresIT {
    @Autowired AppUserJpaRepository users;
    @Autowired JogadorRepository jogadores;
    @Autowired AtributoJpaRepository atributos;
    @Autowired AtividadeConfigRepository configs;
    @Autowired FatorCalculoRepository fatores;
    @Autowired RegistroAtividadeJpaRepository registros;
    @Autowired ProgressionExternalExecutionJpaRepository executions;
    @Autowired ActivityProgressionExecutionStore executionStore;
    @Autowired VersionedProgressionConfigurationResolver resolver;
    @Autowired ActivityProgressionAdapter adapter;
    @Autowired ActivityProgressionRecovery recovery;
    @Autowired CreateRegistroAtividadeService creator;
    @SpyBean ProcessarRegistroAtividadeService legacyProcessor;
    @SpyBean ConfiguredStatefulProgressionApplicationService progression;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void isolateExecutions() {
        executions.deleteAll();
    }

    @Test
    void persistsFrozenIntentAndSameActivityIsIdempotentConcurrently() throws Exception {
        var fixture = fixture();
        var activity = intent(fixture, 30);

        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = List.of(pool.submit(() -> adapter.process(activity.id().getValue())),
                    pool.submit(() -> adapter.process(activity.id().getValue())));
            assertThat(futures.get(0).get()).isTrue();
            assertThat(futures.get(1).get()).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(xp(fixture.userId())).isEqualTo(30L);
        assertThat(executions.findBySourceSystemAndIdempotencyKey(ActivityProgressionAdapter.SOURCE,
                activity.id().getValue().toString())).hasValueSatisfying(e -> {
            assertThat(e.getProcessingStatus()).isEqualTo("COMPLETED");
            assertThat(e.getAttemptCount()).isZero();
            assertThat(e.getRequestJson()).contains("30");
        });
        assertThat(registros.findById(activity.id())).hasValueSatisfying(r -> {
            assertThat(r.getStatusProcessamento().name()).isEqualTo("PROCESSADO");
            assertThat(r.getConfigurationVersionId()).isNotNull();
            assertThat(r.getSkillPolicyVersionId()).isNotNull();
        });
    }

    @Test
    void productionActivityCreationDispatchesOnlyModernProgressionAfterCommit() {
        var fixture = fixture();
        String email = jdbc.queryForObject("SELECT email FROM app_user WHERE id = ?", String.class, fixture.userId());
        creator.create(new CreateRegistroAtividadeCommand(email, fixture.config().getId().getValue(),
                LocalDateTime.now().minusHours(1), LocalDateTime.now(),
                List.of(new DetalheRegistroRequest(fixture.factor().getId().getValue(), "1"))));

        UUID activityId = jdbc.queryForObject("SELECT r.id FROM registro_atividade r "
                + "JOIN jogador j ON j.id = r.jogador_id WHERE j.user_id = ?", UUID.class, fixture.userId());

        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(executions.findBySourceSystemAndIdempotencyKey(ActivityProgressionAdapter.SOURCE,
                    activityId.toString())).hasValueSatisfying(execution ->
                    assertThat(execution.getProcessingStatus()).isEqualTo("COMPLETED"));
            assertThat(xp(fixture.userId())).isEqualTo(1L);
            assertThat(jdbc.queryForObject("SELECT status_processamento FROM registro_atividade WHERE id = ?",
                    String.class, activityId)).isEqualTo("PROCESSADO");
        });

        verify(legacyProcessor, never()).processar(any());
    }

    @Test
    void distinctActivitiesForSameSubjectBothContribute() throws Exception {
        var fixture = fixture();
        var first = intent(fixture, 30);
        var second = intent(fixture, 20);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = List.of(pool.submit(() -> adapter.process(first.id().getValue())),
                    pool.submit(() -> adapter.process(second.id().getValue())));
            assertThat(futures.get(0).get()).isTrue();
            assertThat(futures.get(1).get()).isTrue();
        } finally {
            pool.shutdownNow();
        }
        assertThat(xp(fixture.userId())).isEqualTo(50L);
        assertThat(executions.findByProcessingStatusInOrderByCreatedAtAsc(List.of("COMPLETED")))
                .extracting(e -> e.getSourceSystem()).contains(ActivityProgressionAdapter.SOURCE);
    }

    @Test
    void changedFrozenInputForSameIdentityIsRejected() {
        var fixture = fixture();
        var activity = intent(fixture, 30);
        var identity = ActivityProgressionAdapter.identity(activity.id().getValue());
        var changedFacts = new ProgressionFact(List.of(new ProgressionFact.Detail(
                fixture.factor().getId().getValue().toString(), 31)));
        var changedFingerprint = ProgressionExecutionFingerprint.ofFrozen(identity,
                new ExternalSubjectReference("logos", fixture.userId().toString()),
                new ExternalProgressionConfigurationReference("activity-" + fixture.config().getId().getValue(), 1),
                changedFacts, fixture.resolved().configurationVersionId(), fixture.resolved().skillPolicyVersionId());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> executionStore.create(identity, changedFingerprint,
                        fixture.userId(), changedFacts, fixture.resolved(),
                        "activity-" + fixture.config().getId().getValue(), 1))
                .isInstanceOfAny(ProgressionExecutionConflictException.class,
                        org.springframework.dao.InvalidDataAccessApiUsageException.class);
        assertThat(executions.findBySourceSystemAndIdempotencyKey(ActivityProgressionAdapter.SOURCE,
                activity.id().getValue().toString()).orElseThrow().getRequestJson()).contains("30");
    }

    @Test
    void recoveryProcessesPersistedUnresolvedIntentAndUpdatesProjection() {
        var fixture = fixture();
        var activity = intent(fixture, 20);

        assertThat(recovery.recover()).isEqualTo(1);
        assertThat(xp(fixture.userId())).isEqualTo(20L);
        assertThat(registros.findById(activity.id())).hasValueSatisfying(r ->
                assertThat(r.getStatusProcessamento().name()).isEqualTo("PROCESSADO"));
    }

    @Test
    void sequentialRetryReusesCompletedExecution() {
        var fixture = fixture();
        var activity = intent(fixture, 30);

        assertThat(adapter.process(activity.id().getValue())).isTrue();
        assertThat(adapter.process(activity.id().getValue())).isTrue();
        assertThat(xp(fixture.userId())).isEqualTo(30L);
    }

    @Test
    void differentSubjectsCanProcessConcurrently() throws Exception {
        var firstFixture = fixture();
        var secondFixture = fixture();
        var first = intent(firstFixture, 30);
        var second = intent(secondFixture, 20);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = List.of(pool.submit(() -> adapter.process(first.id().getValue())),
                    pool.submit(() -> adapter.process(second.id().getValue())));
            assertThat(futures.get(0).get()).isTrue();
            assertThat(futures.get(1).get()).isTrue();
        } finally {
            pool.shutdownNow();
        }
        assertThat(xp(firstFixture.userId())).isEqualTo(30L);
        assertThat(xp(secondFixture.userId())).isEqualTo(20L);
    }

    @Test
    void canonicalMutationRollsBackWhenProjectionCannotBeWritten() {
        var fixture = fixture();
        var activity = intent(fixture, 30);
        jdbc.update("DELETE FROM registro_atividade_detalhe WHERE registro_atividade_id = ?", activity.id().getValue());
        jdbc.update("DELETE FROM registro_atividade WHERE id = ?", activity.id().getValue());

        assertThatThrownBy(() -> adapter.process(activity.id().getValue()))
                .isInstanceOf(RuntimeException.class);
        assertThat(xp(fixture.userId())).isZero();
        assertThat(executions.findBySourceSystemAndIdempotencyKey(ActivityProgressionAdapter.SOURCE,
                activity.id().getValue().toString())).hasValueSatisfying(e ->
                assertThat(e.getProcessingStatus()).isEqualTo("PENDING"));
    }

    @Test
    void retryUsesFrozenConfigurationAndSkillPolicyAfterRollout() {
        var fixture = fixture();
        var activity = intent(fixture, 30);
        var execution = executions.findBySourceSystemAndIdempotencyKey(ActivityProgressionAdapter.SOURCE,
                activity.id().getValue().toString()).orElseThrow();
        var configurationVersion = execution.getConfigurationVersionId();
        var skillPolicyVersion = execution.getSkillPolicyVersionId();

        doThrow(new IllegalStateException("controlled retry failure")).when(progression)
                .executeResolved(any(), any(), any());
        try {
            assertThatThrownBy(() -> adapter.process(activity.id().getValue()))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            reset(progression);
        }

        rolloutConfiguration(configurationVersion, 99);
        rolloutSkillPolicy(skillPolicyVersion);

        assertThat(recovery.recover()).isEqualTo(1);
        assertThat(xp(fixture.userId())).isEqualTo(30L);
        assertThat(executions.findBySourceSystemAndIdempotencyKey(ActivityProgressionAdapter.SOURCE,
                activity.id().getValue().toString())).hasValueSatisfying(recovered -> {
            assertThat(recovered.getConfigurationVersionId()).isEqualTo(configurationVersion);
            assertThat(recovered.getSkillPolicyVersionId()).isEqualTo(skillPolicyVersion);
            assertThat(recovered.getProcessingStatus()).isEqualTo("COMPLETED");
        });
    }

    @Test
    void fixedRulesCoverMatchedUnmatchedInclusiveOpenAndTruncatedActivityPath() {
        var lower = fixture();
        var unmatched = fixture();
        var upper = fixture();
        var open = fixture();
        assertThat(process(lower, fixed(lower, 100, 0, .25, 2, 80.0, 120.0), 80)).isEqualTo(50L);
        assertThat(process(unmatched, fixed(unmatched, 100, 0, .50, 2, 80.0, 120.0), 79.9)).isEqualTo(25L);
        assertThat(process(upper, fixed(upper, 100, 0, .25, 2, 80.0, 120.0), 120)).isEqualTo(50L);
        assertThat(process(open, fixed(open, 101, 0, .25, 1.5, null, null), 999)).isEqualTo(37L);
    }

    @Test
    void factValueRulesCoverMatchedAndUnmatchedActivityPath() {
        var matched = fixture();
        var unmatched = fixture();
        assertThat(process(matched, factValue(matched, 1, 0, 1, 1, 0.0, null), 30)).isEqualTo(30L);
        assertThat(process(unmatched, factValue(unmatched, 100, 0, .50, 2, 80.0, 120.0), 10)).isEqualTo(250L);
    }

    @Test
    void stressRulesAndFloorAreAppliedThroughActivityPath() {
        var positiveNegative = fixture();
        var key = attributeKey(positiveNegative);
        var config = new ProgressionConfiguration(10, 10,
                List.of(new ProgressionConfiguration.AttributeDistribution(key, 1,
                        List.of(xpRule(positiveNegative, 1, null, null, XpCalculationMode.FIXED)),
                        List.of(new ProgressionConfiguration.StressRule(2, 0.0, null, ProgressionConfiguration.StressType.POSITIVE),
                                new ProgressionConfiguration.StressRule(1, 0.0, null, ProgressionConfiguration.StressType.NEGATIVE)))), List.of());
        process(positiveNegative, config, 1);
        assertThat(stress(positiveNegative.userId())).isEqualTo(5);

        var floor = fixture();
        jdbc.update("UPDATE estresse_global SET pontuacao_atual = 2 WHERE jogador_id = (SELECT id FROM jogador WHERE user_id = ?)", floor.userId());
        var floorConfig = new ProgressionConfiguration(1, 10,
                List.of(new ProgressionConfiguration.AttributeDistribution(attributeKey(floor), 1,
                        List.of(xpRule(floor, 1, null, null, XpCalculationMode.FIXED)),
                        List.of(new ProgressionConfiguration.StressRule(.5, 0.0, null, ProgressionConfiguration.StressType.NEGATIVE)))), List.of());
        process(floor, floorConfig, 1);
        assertThat(stress(floor.userId())).isZero();
    }

    @Test
    void distributesXpAcrossTwoAttributesWithoutChangingGlobalXp() {
        var fixture = fixture();
        var secondAttribute = addAttribute(fixture, "CREATIVITY");
        var factorKey = fixture.factor().getId().getValue().toString();
        var configuration = new ProgressionConfiguration(100, 0,
                List.of(new ProgressionConfiguration.AttributeDistribution(attributeKey(fixture), .60,
                                List.of(new ProgressionConfiguration.XpRule(factorKey, 1, null, null, XpCalculationMode.FIXED)), List.of()),
                        new ProgressionConfiguration.AttributeDistribution(secondAttribute, .40,
                                List.of(new ProgressionConfiguration.XpRule(factorKey, 1, null, null, XpCalculationMode.FIXED)), List.of())),
                List.of());

        var run = processActivity(fixture, configuration, List.of(new FactValue(fixture.factor(), 1)));

        assertThat(run.globalXp()).isEqualTo(100L);
        assertThat(attributeXp(fixture.userId(), attributeKey(fixture))).isEqualTo(60L);
        assertThat(attributeXp(fixture.userId(), secondAttribute)).isEqualTo(40L);
        assertProjection(run, fixture, 100, 0);
    }

    @Test
    void accumulatesMultipleActivityDetailsAndFactorsInOneExecution() {
        var fixture = fixture();
        var secondFactor = fatores.save(new FatorCalculo(FatorCalculoId.generate(), "minutes-" + UUID.randomUUID(), "minutes", TipoInput.NUMERICO,
                "minutes_" + UUID.randomUUID().toString().replace("-", "")));
        var attributeKey = attributeKey(fixture);
        var configuration = new ProgressionConfiguration(10, 0,
                List.of(new ProgressionConfiguration.AttributeDistribution(attributeKey, 1,
                        List.of(new ProgressionConfiguration.XpRule(fixture.factor().getId().getValue().toString(), 1, null, null, XpCalculationMode.FIXED),
                                new ProgressionConfiguration.XpRule(secondFactor.getId().getValue().toString(), 1, null, null, XpCalculationMode.FIXED)),
                        List.of())), List.of());

        var run = processActivity(fixture, configuration,
                List.of(new FactValue(fixture.factor(), 2), new FactValue(secondFactor, 3)));

        assertThat(run.globalXp()).isEqualTo(20L);
        assertThat(attributeXp(fixture.userId(), attributeKey)).isEqualTo(20L);
        assertProjection(run, fixture, 20, 0);
    }

    @Test
    void skillBonusAppliesOnlyToAttributeXpThroughActivityPath() {
        var fixture = fixture();
        var skillKey = addSkillAtLevel(fixture, 3);
        var key = attributeKey(fixture);
        var configuration = new ProgressionConfiguration(100, 0,
                List.of(new ProgressionConfiguration.AttributeDistribution(key, 1,
                        List.of(xpRule(fixture, 1, null, null, XpCalculationMode.FIXED)), List.of())),
                List.of(new ProgressionConfiguration.SkillBonusRule(skillKey, key, 10)));

        var run = processActivity(fixture, configuration, List.of(new FactValue(fixture.factor(), 1)));

        assertThat(run.globalXp()).isEqualTo(100L);
        assertThat(attributeXp(fixture.userId(), key)).isEqualTo(101L);
        assertProjection(run, fixture, 100, 0);
    }

    @Test
    void singleLevelTransitionGrantsOneSkillPointThroughActivityPath() {
        var fixture = fixture();
        setGlobalState(fixture, 149, 1, 1);

        var run = processActivity(fixture, globalOnly(150), List.of(new FactValue(fixture.factor(), 1)));

        assertThat(run.globalXp()).isEqualTo(299L);
        assertThat(globalLevel(fixture.userId())).isEqualTo(2);
        assertThat(skillPoints(fixture.userId())).isEqualTo(2);
        assertProjection(run, fixture, 150, 0);
    }

    @Test
    void multiLevelTransitionUsesCanonicalRepeatedThresholdPolicy() {
        var fixture = fixture();
        setGlobalState(fixture, 0, 1, 1);

        var run = processActivity(fixture, globalOnly(1600), List.of(new FactValue(fixture.factor(), 1)));

        assertThat(run.globalXp()).isEqualTo(1600L);
        assertThat(globalLevel(fixture.userId())).isEqualTo(4);
        assertThat(skillPoints(fixture.userId())).isEqualTo(4);
        assertProjection(run, fixture, 1600, 0);
    }

    private long process(Fixture fixture, ProgressionConfiguration configuration, double value) {
        return processActivity(fixture, configuration, List.of(new FactValue(fixture.factor(), value))).globalXp();
    }

    private ProcessedActivity processActivity(Fixture fixture, ProgressionConfiguration configuration, List<FactValue> values) {
        var activity = activity(fixture, values);
        var resolved = new ResolvedProgressionConfiguration(fixture.resolved().configurationVersionId(),
                fixture.resolved().skillPolicyVersionId(), configuration,
                values.stream().map(value -> value.factor().getId().getValue().toString()).collect(java.util.stream.Collectors.toSet()));
        var identity = ActivityProgressionAdapter.identity(activity.id().getValue());
        var facts = new ProgressionFact(values.stream()
                .map(value -> new ProgressionFact.Detail(value.factor().getId().getValue().toString(), value.value())).toList());
        var fingerprint = ProgressionExecutionFingerprint.ofFrozen(identity,
                new ExternalSubjectReference("logos", fixture.userId().toString()),
                new ExternalProgressionConfigurationReference("activity-" + fixture.config().getId().getValue(), 1), facts,
                resolved.configurationVersionId(), resolved.skillPolicyVersionId());
        executionStore.create(identity, fingerprint, fixture.userId(), facts, resolved,
                "activity-" + fixture.config().getId().getValue(), 1);
        assertThat(adapter.process(activity.id().getValue())).isTrue();
        assertThat(registros.findById(activity.id())).hasValueSatisfying(record -> {
            assertThat(record.getStatusProcessamento().name()).isEqualTo("PROCESSADO");
            assertThat(record.getConfigurationVersionId()).isEqualTo(resolved.configurationVersionId());
            assertThat(record.getSkillPolicyVersionId()).isEqualTo(resolved.skillPolicyVersionId());
        });
        return new ProcessedActivity(activity.id(), xp(fixture.userId()));
    }

    private ProgressionConfiguration globalOnly(int baseXp) {
        return new ProgressionConfiguration(baseXp, 0, List.of(), List.of());
    }

    private ProgressionConfiguration fixed(Fixture fixture, int baseXp, int baseStress, double weight, double multiplier,
                                            Double lower, Double upper) {
        return configuration(fixture, baseXp, baseStress, weight,
                xpRule(fixture, multiplier, lower, upper, XpCalculationMode.FIXED), List.of());
    }

    private ProgressionConfiguration factValue(Fixture fixture, int baseXp, int baseStress, double weight, double multiplier,
                                                Double lower, Double upper) {
        return configuration(fixture, baseXp, baseStress, weight,
                xpRule(fixture, multiplier, lower, upper, XpCalculationMode.FACT_VALUE), List.of());
    }

    private ProgressionConfiguration configuration(Fixture fixture, int baseXp, int baseStress, double weight,
                                                    ProgressionConfiguration.XpRule rule,
                                                    List<ProgressionConfiguration.StressRule> stressRules) {
        return new ProgressionConfiguration(baseXp, baseStress,
                List.of(new ProgressionConfiguration.AttributeDistribution(attributeKey(fixture), weight,
                        List.of(rule), stressRules)), List.of());
    }

    private ProgressionConfiguration.XpRule xpRule(Fixture fixture, double multiplier, Double lower, Double upper,
                                                     XpCalculationMode mode) {
        return new ProgressionConfiguration.XpRule(fixture.factor().getId().getValue().toString(), multiplier, lower, upper, mode);
    }

    private String attributeKey(Fixture fixture) {
        return fixture.resolved().configuration().attributeDistributions().get(0).attributeKey();
    }

    private int stress(UUID userId) {
        return jdbc.queryForObject("SELECT e.pontuacao_atual FROM estresse_global e JOIN jogador j ON j.id = e.jogador_id WHERE j.user_id = ?",
                Integer.class, userId);
    }

    private String addAttribute(Fixture fixture, String name) {
        var attribute = atributos.save(new Atributo(new AtributoId(), name + "-" + UUID.randomUUID(), ""));
        jdbc.update("INSERT INTO atributo_jogador (id, jogador_id, atributo_id, xp_total, nivel_atual) VALUES (?, (SELECT id FROM jogador WHERE user_id = ?), ?, 0, 1)",
                UUID.randomUUID(), fixture.userId(), attribute.getId().getValue());
        return attribute.getId().getValue().toString();
    }

    private String addSkillAtLevel(Fixture fixture, int level) {
        UUID skillId = UUID.randomUUID();
        jdbc.update("INSERT INTO habilidade (id, nome, descricao) VALUES (?, ?, ?)", skillId, "skill-" + UUID.randomUUID(), "");
        jdbc.update("INSERT INTO habilidade_jogador (id, jogador_id, habilidade_id, nivel_atual) VALUES (?, (SELECT id FROM jogador WHERE user_id = ?), ?, ?)",
                UUID.randomUUID(), fixture.userId(), skillId, level);
        return skillId.toString();
    }

    private long attributeXp(UUID userId, String attributeKey) {
        return jdbc.queryForObject("SELECT aj.xp_total FROM atributo_jogador aj JOIN jogador j ON j.id = aj.jogador_id WHERE j.user_id = ? AND aj.atributo_id = ?",
                Long.class, userId, UUID.fromString(attributeKey));
    }

    private void setGlobalState(Fixture fixture, long xp, int level, int points) {
        jdbc.update("UPDATE jogador SET xp_total = ?, nivel_atual = ?, pontos_habilidade = ? WHERE user_id = ?", xp, level, points, fixture.userId());
    }

    private int globalLevel(UUID userId) {
        return jdbc.queryForObject("SELECT nivel_atual FROM jogador WHERE user_id = ?", Integer.class, userId);
    }

    private int skillPoints(UUID userId) {
        return jdbc.queryForObject("SELECT pontos_habilidade FROM jogador WHERE user_id = ?", Integer.class, userId);
    }

    private void assertProjection(ProcessedActivity run, Fixture fixture, int xpGained, int stressGenerated) {
        assertThat(registros.findById(run.activityId())).hasValueSatisfying(record -> {
            assertThat(record.getXpGanhoFinal()).isEqualTo(xpGained);
            assertThat(record.getEstresseGerado()).isEqualTo(stressGenerated);
            assertThat(record.getStatusProcessamento().name()).isEqualTo("PROCESSADO");
            assertThat(record.getConfigurationVersionId()).isEqualTo(fixture.resolved().configurationVersionId());
            assertThat(record.getSkillPolicyVersionId()).isEqualTo(fixture.resolved().skillPolicyVersionId());
        });
    }

    private void rolloutConfiguration(UUID previousVersion, int newBaseXp) {
        UUID definition = jdbc.queryForObject(
                "SELECT definition_id FROM progression_configuration_version WHERE id = ?", UUID.class, previousVersion);
        int revision = jdbc.queryForObject(
                "SELECT COALESCE(MAX(revision), 0) + 1 FROM progression_configuration_version WHERE definition_id = ?",
                Integer.class, definition);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress) "
                + "SELECT ?, definition_id, ?, ?, base_stress FROM progression_configuration_version WHERE id = ?",
                version, revision, newBaseXp, previousVersion);
        UUID distribution = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_version_distribution(id, configuration_version_id, attribute_key, weight) "
                + "SELECT ?, ?, attribute_key, weight FROM progression_configuration_version_distribution WHERE configuration_version_id = ?",
                distribution, version, previousVersion);
        jdbc.update("INSERT INTO progression_configuration_version_xp_rule(id, distribution_id, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode) "
                + "SELECT gen_random_uuid(), ?, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode "
                + "FROM progression_configuration_version_xp_rule r JOIN progression_configuration_version_distribution d ON d.id = r.distribution_id "
                + "WHERE d.configuration_version_id = ?", distribution, previousVersion);
        jdbc.update("INSERT INTO progression_configuration_version_factor(id, configuration_version_id, factor_key, tipo_input) "
                + "SELECT gen_random_uuid(), ?, factor_key, tipo_input FROM progression_configuration_version_factor WHERE configuration_version_id = ?",
                version, previousVersion);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?", version, definition);
    }

    private void rolloutSkillPolicy(UUID previousVersion) {
        UUID policy = jdbc.queryForObject("SELECT policy_id FROM progression_skill_policy_version WHERE id = ?", UUID.class, previousVersion);
        UUID version = UUID.randomUUID();
        int revision = jdbc.queryForObject(
                "SELECT COALESCE(MAX(revision), 0) + 1 FROM progression_skill_policy_version WHERE policy_id = ?",
                Integer.class, policy);
        jdbc.update("INSERT INTO progression_skill_policy_version(id, policy_id, revision) VALUES (?, ?, ?)", version, policy, revision);
        jdbc.update("INSERT INTO progression_skill_policy_version_rule(id, policy_version_id, skill_key, attribute_key, distribution_weight) "
                + "SELECT gen_random_uuid(), ?, skill_key, attribute_key, distribution_weight FROM progression_skill_policy_version_rule WHERE policy_version_id = ?",
                version, previousVersion);
        jdbc.update("UPDATE progression_skill_policy SET current_version_id = ? WHERE id = ?", version, policy);
    }

    private Fixture fixture() {
        var user = users.saveAndFlush(new AppUser(new AppUserId(), "activity-" + UUID.randomUUID() + "@test", encoder.encode("password")));
        var jogador = new Jogador(JogadorId.generate(), user, "activity-player-" + UUID.randomUUID());
        jogador.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), jogador));
        jogadores.save(jogador);
        var learning = atributos.findAll().stream().filter(a -> "LEARNING".equals(a.getNome())).findFirst().orElseThrow();
        jdbc.update("INSERT INTO atributo_jogador (id, jogador_id, atributo_id, xp_total, nivel_atual) VALUES (?, ?, ?, 0, 1)",
                UUID.randomUUID(), jogador.getId().getValue(), learning.getId().getValue());
        var factor = fatores.save(new FatorCalculo(FatorCalculoId.generate(), "pages-" + UUID.randomUUID(), "pages", TipoInput.NUMERICO,
                "pages_" + UUID.randomUUID().toString().replace("-", "")));
        var config = new AtividadeConfig(new AtividadeConfigId(), "activity-" + UUID.randomUUID(), "fixture", 1, 0, null, null);
        var distribution = new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), config, learning, 1.0);
        distribution.adicionarRegraFatorXPS(new RegraFatorXP(new RegraFatorXPId(), distribution, factor, 1.0, 0.0, null));
        config.adicionarRegraDistribuicao(distribution);
        configs.save(config);
        var resolved = resolver.resolveVersioned(new ProgressionConfigurationReference(config.getId().getValue())).orElseThrow();
        jdbc.update("UPDATE progression_configuration_version_xp_rule SET calculation_mode = 'FACT_VALUE' "
                        + "WHERE distribution_id IN (SELECT id FROM progression_configuration_version_distribution WHERE configuration_version_id = ?)",
                resolved.configurationVersionId());
        resolved = resolver.resolveVersioned(new ProgressionConfigurationReference(config.getId().getValue())).orElseThrow();
        return new Fixture(user.getId().getValue(), config, factor, resolved);
    }

    private Activity activity(Fixture fixture, double value) {
        return activity(fixture, List.of(new FactValue(fixture.factor(), value)));
    }

    private Activity activity(Fixture fixture, List<FactValue> values) {
        var record = new RegistroAtividade(RegistroAtividadeId.generate(),
                jogadores.findByAppUserId(new AppUserId(fixture.userId())).orElseThrow(), fixture.config(),
                LocalDateTime.now().minusHours(1), LocalDateTime.now());
        values.forEach(value -> record.adicionarDetalhe(value.factor(), Double.toString(value.value())));
        registros.save(record);
        return new Activity(record.getId(), fixture);
    }

    private Activity intent(Fixture fixture, double value) {
        var activity = activity(fixture, value);
        var identity = ActivityProgressionAdapter.identity(activity.id().getValue());
        var facts = new ProgressionFact(List.of(new ProgressionFact.Detail(
                fixture.factor().getSemanticKey(), value)));
        var fingerprint = ProgressionExecutionFingerprint.ofFrozen(identity,
                new ExternalSubjectReference("logos", fixture.userId().toString()),
                new ExternalProgressionConfigurationReference("activity-" + fixture.config().getId().getValue(), 1), facts,
                fixture.resolved().configurationVersionId(), fixture.resolved().skillPolicyVersionId());
        executionStore.create(identity, fingerprint, fixture.userId(), facts, fixture.resolved(),
                "activity-" + fixture.config().getId().getValue(), 1);
        return activity;
    }

    private long xp(UUID userId) {
        return jdbc.queryForObject("SELECT xp_total FROM jogador WHERE user_id = ?", Long.class, userId);
    }

    private record Fixture(UUID userId, AtividadeConfig config, FatorCalculo factor,
                           com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration resolved) {}
    private record Activity(RegistroAtividadeId id, Fixture fixture) {}
    private record FactValue(FatorCalculo factor, double value) {}
    private record ProcessedActivity(RegistroAtividadeId activityId, long globalXp) {}
}
