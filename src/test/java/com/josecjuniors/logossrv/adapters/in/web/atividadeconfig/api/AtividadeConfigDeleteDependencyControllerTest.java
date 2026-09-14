package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendada;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendadaId;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.repository.AtividadeAgendadaRepository;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AtividadeConfigDeleteDependencyControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AtividadeConfigRepository activityRepository;
    @Autowired
    private AtividadeAgendadaRepository scheduleRepository;
    @Autowired
    private RegistroAtividadeRepository registrationRepository;
    @Autowired
    private JogadorRepository playerRepository;
    @Autowired
    private AppUserJpaRepository userRepository;
    @Autowired
    private AtributoRepository attributeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbc;

    private String token;

    @BeforeEach
    void setUp() {
        var user = userRepository.save(new AppUser(
                new AppUserId(),
                "delete-dependency-" + UUID.randomUUID() + "@test.local",
                passwordEncoder.encode("password")));
        token = jwtService.generateToken(user);
    }

    @Test
    void modernProgressionDefinition_blocksDeleteAndPreservesBinding() throws Exception {
        AtividadeConfig activity = activityRepository.save(new AtividadeConfig(
                new AtividadeConfigId(), "Definition guarded", "Activity"));
        UUID definitionId = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition "
                        + "(id, logical_key, legacy_atividade_config_id, current_version_id) VALUES (?, ?, ?, NULL)",
                definitionId, "test-definition-" + UUID.randomUUID(), activity.getId().getValue());

        deleteActivity(activity.getId().getValue());

        assertThat(activityRepository.findById(activity.getId())).isPresent();
        assertThat(jdbc.queryForObject("SELECT legacy_atividade_config_id FROM progression_configuration_definition WHERE id = ?",
                UUID.class, definitionId)).isEqualTo(activity.getId().getValue());
    }

    @Test
    void registration_blocksDeleteAndPreservesHistory() throws Exception {
        AtividadeConfig activity = activityRepository.save(new AtividadeConfig(
                new AtividadeConfigId(), "Registration guarded", "Activity"));
        AppUser user = userRepository.save(new AppUser(
                new AppUserId(), "registration-" + UUID.randomUUID() + "@test.local", passwordEncoder.encode("password")));
        Jogador player = playerRepository.save(new Jogador(JogadorId.generate(), user, "player-" + UUID.randomUUID()));
        RegistroAtividade registration = registrationRepository.save(new RegistroAtividade(
                RegistroAtividadeId.generate(), player, activity,
                LocalDateTime.now().minusHours(1), LocalDateTime.now()));

        deleteActivity(activity.getId().getValue());

        assertThat(activityRepository.findById(activity.getId())).isPresent();
        assertThat(registrationRepository.findById(registration.getId())).isPresent();
    }

    @Test
    void schedule_blocksDeleteAndPreservesSchedule() throws Exception {
        AtividadeConfig activity = activityRepository.save(new AtividadeConfig(
                new AtividadeConfigId(), "Schedule guarded", "Activity"));
        AppUser user = userRepository.save(new AppUser(
                new AppUserId(), "schedule-" + UUID.randomUUID() + "@test.local", passwordEncoder.encode("password")));
        Jogador player = playerRepository.save(new Jogador(JogadorId.generate(), user, "scheduler-" + UUID.randomUUID()));
        AtividadeAgendada schedule = scheduleRepository.save(new AtividadeAgendada(
                AtividadeAgendadaId.generate(), player, activity,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)));

        deleteActivity(activity.getId().getValue());

        assertThat(activityRepository.findById(activity.getId())).isPresent();
        assertThat(scheduleRepository.findById(schedule.getId())).isPresent();
    }

    @Test
    void transitionalLegacyDistributionRow_blocksDeleteWithoutLegacyEntity() throws Exception {
        AtividadeConfig activity = activityRepository.save(new AtividadeConfig(
                new AtividadeConfigId(), "Legacy guard", "Activity"));
        Atributo attribute = attributeRepository.save(new Atributo(
                new AtributoId(), "attribute-" + UUID.randomUUID(), "Test attribute"));
        UUID ruleId = UUID.randomUUID();
        jdbc.update("INSERT INTO regra_distribuicao_atividade "
                        + "(id, atividade_config_id, atributo_id, peso_percentual) VALUES (?, ?, ?, ?)",
                ruleId, activity.getId().getValue(), attribute.getId().getValue(), 1.0);

        deleteActivity(activity.getId().getValue());

        assertThat(activityRepository.findById(activity.getId())).isPresent();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM regra_distribuicao_atividade WHERE id = ?",
                Integer.class, ruleId)).isEqualTo(1);
    }

    private void deleteActivity(UUID activityId) throws Exception {
        mockMvc.perform(delete("/api/atividades-config/{id}", activityId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }
}
