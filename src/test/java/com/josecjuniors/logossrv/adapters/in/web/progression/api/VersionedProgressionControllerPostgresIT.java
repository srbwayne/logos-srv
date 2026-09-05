package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.VersionedProgressionEvaluationRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.adapters.out.atividadeconfig.jpa.AtividadeConfigJpaRepository;
import com.josecjuniors.logossrv.adapters.out.estresseglobal.jpa.EstresseGlobalJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.JogadorJpaRepository;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class VersionedProgressionControllerPostgresIT {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserJpaRepository users;
    @Autowired JogadorJpaRepository jogadores;
    @Autowired EstresseGlobalJpaRepository estresses;
    @Autowired AtividadeConfigJpaRepository configs;
    @Autowired ProgressionSubjectIdentityJpaRepository identities;
    @Autowired ProgressionConfigurationResolver resolver;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;

    private Jogador player;
    private String key;
    private String token;
    private UUID subjectId;

    @BeforeEach
    void setUp() {
        identities.deleteAll();
        estresses.deleteAll();
        jogadores.deleteAll();
        configs.deleteAll();
        users.deleteAll();
        var user = users.save(new AppUser(new AppUserId(), "versioned-config@example.test", encoder.encode("password")));
        subjectId = user.getId().getValue();
        player = new Jogador(JogadorId.generate(), user, "versioned-config-player");
        player.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), player));
        jogadores.saveAndFlush(player);
        identities.saveAndFlush(new ProgressionSubjectIdentity(java.util.UUID.randomUUID(), "experiment", "versioned-player", player));
        var config = configs.saveAndFlush(new AtividadeConfig(new AtividadeConfigId(), "Versioned", "fixture", 10, 0, null, null));
        resolver.resolve(new ProgressionConfigurationReference(config.getId().getValue()));
        key = "legacy:" + config.getId().getValue();
        token = jwt.generateToken(user);
    }

    @Test
    void resolvesCurrentConfigurationByKey() throws Exception {
        perform(new VersionedProgressionEvaluationRequest(new VersionedProgressionEvaluationRequest.ConfigurationReference(key.toUpperCase(Locale.ROOT), null), List.of()), subjectId)
                .andExpect(status().isOk());
    }

    @Test
    void resolvesExactConfigurationRevision() throws Exception {
        perform(new VersionedProgressionEvaluationRequest(new VersionedProgressionEvaluationRequest.ConfigurationReference(key, 1), List.of()), subjectId)
                .andExpect(status().isOk());
    }

    @Test
    void resolvesExternalSubjectWithConfigurationKey() throws Exception {
        performExternal(new VersionedProgressionEvaluationRequest(new VersionedProgressionEvaluationRequest.ConfigurationReference(key, null), List.of()))
                .andExpect(status().isOk());
    }

    @Test
    void returnsNotFoundForUnknownConfigurationKey() throws Exception {
        perform(new VersionedProgressionEvaluationRequest(new VersionedProgressionEvaluationRequest.ConfigurationReference("missing", null), List.of()), subjectId)
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForUnknownConfigurationRevision() throws Exception {
        perform(new VersionedProgressionEvaluationRequest(new VersionedProgressionEvaluationRequest.ConfigurationReference(key, 999), List.of()), subjectId)
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestForInvalidConfigurationRevision() throws Exception {
        perform(new VersionedProgressionEvaluationRequest(new VersionedProgressionEvaluationRequest.ConfigurationReference(key, 0), List.of()), subjectId)
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions perform(VersionedProgressionEvaluationRequest request, java.util.UUID subject) throws Exception {
        return mockMvc.perform(post("/api/internal/v2/progression/{subjectId}/evaluate", subject)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions performExternal(VersionedProgressionEvaluationRequest request) throws Exception {
        return mockMvc.perform(post("/api/internal/v2/progression/external/experiment/versioned-player/evaluate")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
