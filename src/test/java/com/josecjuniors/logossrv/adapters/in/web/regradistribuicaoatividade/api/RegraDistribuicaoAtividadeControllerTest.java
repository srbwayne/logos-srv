package com.josecjuniors.logossrv.adapters.in.web.regradistribuicaoatividade.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaoatividade.dto.request.CreateRegraDistribuicaoRequest;
import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaoatividade.dto.request.UpdateRegraDistribuicaoRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class RegraDistribuicaoAtividadeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegraDistribuicaoAtividadeRepository regraRepository;
    @Autowired
    private AtividadeConfigRepository atividadeConfigRepository;
    @Autowired
    private AtributoRepository atributoRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private AtividadeConfig testAtividade;
    private Atributo testAtributo;

    @BeforeEach
    void setUp() {
        regraRepository.deleteAll();
        atividadeConfigRepository.deleteAll();
        atributoRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser testAppUser = new AppUser(new AppUserId(), "regra.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);

        testAtividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida", "Corrida de rua", 100, 10, null, null));
        testAtributo = atributoRepository.save(new Atributo(new AtributoId(), "Resistência", "Capacidade de manter o esforço"));
    }

    // --- POST /api/atividades-config/{atividadeId}/regras-distribuicao ---
    @Test
    void create_withValidData_shouldReturn201() throws Exception {
        CreateRegraDistribuicaoRequest request = new CreateRegraDistribuicaoRequest(testAtributo.getId().getValue(), 0.75);

        mockMvc.perform(post("/api/atividades-config/{atividadeId}/regras-distribuicao", testAtividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.atividadeConfigNome").value(testAtividade.getNome()))
                .andExpect(jsonPath("$.atributoNome").value(testAtributo.getNome()))
                .andExpect(jsonPath("$.pesoPercentual").value(0.75));
    }

    @Test
    void create_whenAtividadeConfigNotFound_shouldReturn404() throws Exception {
        CreateRegraDistribuicaoRequest request = new CreateRegraDistribuicaoRequest(testAtributo.getId().getValue(), 0.5);

        mockMvc.perform(post("/api/atividades-config/{atividadeId}/regras-distribuicao", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }

    @Test
    void create_whenAtributoNotFound_shouldReturn404() throws Exception {
        CreateRegraDistribuicaoRequest request = new CreateRegraDistribuicaoRequest(UUID.randomUUID(), 0.5);

        mockMvc.perform(post("/api/atividades-config/{atividadeId}/regras-distribuicao", testAtividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }

    @Test
    void create_withoutToken_shouldReturn403() throws Exception {
        CreateRegraDistribuicaoRequest request = new CreateRegraDistribuicaoRequest(testAtributo.getId().getValue(), 0.5);

        mockMvc.perform(post("/api/atividades-config/{atividadeId}/regras-distribuicao", testAtividade.getId().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/atividades-config/{atividadeId}/regras-distribuicao ---
    @Test
    void getAllByAtividade_whenRulesExist_shouldReturn200AndListOfRules() throws Exception {
        Atributo outroAtributo = atributoRepository.save(new Atributo(new AtributoId(), "Força", ""));
        regraRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), testAtividade, testAtributo, 0.7));
        regraRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), testAtividade, outroAtributo, 0.3));

        mockMvc.perform(get("/api/atividades-config/{atividadeId}/regras-distribuicao", testAtividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].atividadeConfigNome").value(testAtividade.getNome()));
    }

    @Test
    void getAllByAtividade_whenNoRulesExist_shouldReturn200AndEmptyList() throws Exception {
        mockMvc.perform(get("/api/atividades-config/{atividadeId}/regras-distribuicao", testAtividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllByAtividade_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/atividades-config/{atividadeId}/regras-distribuicao", testAtividade.getId().getValue()))
                .andExpect(status().isForbidden());
    }

    // --- PUT /api/atividades-config/{atividadeId}/regras-distribuicao/{regraDistribuicaoHabilidadeId} ---
    @Test
    void update_withValidData_shouldReturn200() throws Exception {
        RegraDistribuicaoAtividade regra = regraRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), testAtividade, testAtributo, 0.5));
        UpdateRegraDistribuicaoRequest request = new UpdateRegraDistribuicaoRequest(0.9);

        mockMvc.perform(put("/api/atividades-config/{atividadeId}/regras-distribuicao/{regraId}", testAtividade.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.pesoPercentual").value(0.9));
    }

    @Test
    void update_whenRegraNotFound_shouldReturn404() throws Exception {
        UpdateRegraDistribuicaoRequest request = new UpdateRegraDistribuicaoRequest(0.9);

        mockMvc.perform(put("/api/atividades-config/{atividadeId}/regras-distribuicao/{regraId}", testAtividade.getId().getValue(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }

    @Test
    void update_withoutToken_shouldReturn403() throws Exception {
        RegraDistribuicaoAtividade regra = regraRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), testAtividade, testAtributo, 0.5));
        UpdateRegraDistribuicaoRequest request = new UpdateRegraDistribuicaoRequest(0.9);

        mockMvc.perform(put("/api/atividades-config/{atividadeId}/regras-distribuicao/{regraId}", testAtividade.getId().getValue(), regra.getId().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /api/atividades-config/{atividadeId}/regras-distribuicao/{regraDistribuicaoHabilidadeId} ---
    @Test
    void delete_whenRegraExists_shouldReturn204() throws Exception {
        RegraDistribuicaoAtividade regra = regraRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), testAtividade, testAtributo, 0.5));

        mockMvc.perform(delete("/api/atividades-config/{atividadeId}/regras-distribuicao/{regraId}", testAtividade.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isGone());
    }

    @Test
    void delete_whenRegraNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/atividades-config/{atividadeId}/regras-distribuicao/{regraId}", testAtividade.getId().getValue(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isGone());
    }

    @Test
    void delete_withoutToken_shouldReturn403() throws Exception {
        RegraDistribuicaoAtividade regra = regraRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), testAtividade, testAtributo, 0.5));

        mockMvc.perform(delete("/api/atividades-config/{atividadeId}/regras-distribuicao/{regraId}", testAtividade.getId().getValue(), regra.getId().getValue()))
                .andExpect(status().isForbidden());
    }
}
