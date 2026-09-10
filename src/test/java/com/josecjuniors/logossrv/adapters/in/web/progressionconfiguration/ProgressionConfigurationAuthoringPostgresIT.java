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
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProgressionConfigurationAuthoringPostgresIT {
    private static final UUID LEARNING_ATTRIBUTE = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FatorCalculoRepository factors;
    @Autowired private ProgressionConfigurationAuthoringService service;
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
}
