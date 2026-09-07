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
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionConflictException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

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
        var factor = fatores.save(new FatorCalculo(FatorCalculoId.generate(), "pages-" + UUID.randomUUID(), "pages", TipoInput.NUMERICO));
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
        var record = new RegistroAtividade(RegistroAtividadeId.generate(),
                jogadores.findByAppUserId(new AppUserId(fixture.userId())).orElseThrow(), fixture.config(),
                LocalDateTime.now().minusHours(1), LocalDateTime.now());
        record.adicionarDetalhe(fixture.factor(), Double.toString(value));
        registros.save(record);
        return new Activity(record.getId(), fixture);
    }

    private Activity intent(Fixture fixture, double value) {
        var activity = activity(fixture, value);
        var identity = ActivityProgressionAdapter.identity(activity.id().getValue());
        var facts = new ProgressionFact(List.of(new ProgressionFact.Detail(
                fixture.factor().getId().getValue().toString(), value)));
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
}
