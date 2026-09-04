package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionEvaluationRequest;
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
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ExternalSubjectProgressionPostgresIT {

    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AppUserJpaRepository appUserRepository;
    @Autowired private JogadorJpaRepository jogadorRepository;
    @Autowired private EstresseGlobalJpaRepository estresseRepository;
    @Autowired private AtividadeConfigJpaRepository configRepository;
    @Autowired private ProgressionSubjectIdentityJpaRepository identityRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private String token;
    private UUID configurationId;

    @BeforeEach
    void setUp() {
        identityRepository.deleteAll();
        estresseRepository.deleteAll();
        jogadorRepository.deleteAll();
        configRepository.deleteAll();
        appUserRepository.deleteAll();

        var user = new AppUser(new AppUserId(USER_ID), "external-http@example.com",
                passwordEncoder.encode("password"));
        appUserRepository.save(user);
        var jogador = new Jogador(JogadorId.generate(), user, "external-http-player");
        jogador.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), jogador));
        jogadorRepository.saveAndFlush(jogador);
        identityRepository.saveAndFlush(new ProgressionSubjectIdentity(
                UUID.randomUUID(), "experiment", "subject-001", jogador));
        var config = configRepository.save(new AtividadeConfig(
                new AtividadeConfigId(), "External HTTP", "fixture", 10, 0, null, null));
        configurationId = config.getId().getValue();
        token = jwtService.generateToken(user);
    }

    @Test
    void executaProgressaoViaHttpIdentityMappingEConfiguredProgressionReais() throws Exception {
        var request = new ProgressionEvaluationRequest(configurationId, List.of());

        mockMvc.perform(post("/api/internal/v1/progression/external/{namespace}/{externalId}/evaluate",
                        " EXPERIMENT ", "subject-001")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.globalXpDelta").value(10))
                .andExpect(jsonPath("$.profile.globalXp").value(10));
    }

    @Test
    void endpointExternoExigeAutenticacaoAtual() throws Exception {
        mockMvc.perform(post("/api/internal/v1/progression/external/experiment/subject-001/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProgressionEvaluationRequest(configurationId, List.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void mappingInexistenteRetornaNotFoundPeloFluxoReal() throws Exception {
        mockMvc.perform(post("/api/internal/v1/progression/external/experiment/missing/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProgressionEvaluationRequest(configurationId, List.of()))))
                .andExpect(status().isNotFound());
    }
}
