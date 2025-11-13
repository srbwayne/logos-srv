package com.josecjuniors.logossrv.adapters.in.web.regrafatorxp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.regrafatorxp.dto.request.CreateRegraFatorXPRequest;
import com.josecjuniors.logossrv.adapters.in.web.regrafatorxp.dto.request.UpdateRegraFatorXPRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.repository.RegraFatorXPRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class RegraFatorXPControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegraFatorXPRepository regraFatorXPRepository;
    @Autowired
    private RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository;
    @Autowired
    private AtividadeConfigRepository atividadeConfigRepository;
    @Autowired
    private AtributoRepository atributoRepository;
    @Autowired
    private FatorCalculoRepository fatorCalculoRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private RegraDistribuicaoAtividade testRegraDistribuicao;
    private FatorCalculo testFatorCalculo;

    @BeforeEach
    void setUp() {
        regraFatorXPRepository.deleteAll();
        regraDistribuicaoRepository.deleteAll();
        atividadeConfigRepository.deleteAll();
        atributoRepository.deleteAll();
        fatorCalculoRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser testAppUser = new AppUser(new AppUserId(), "regra-fator.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);

        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida", null, 100, 10, null, null));
        Atributo atributo = atributoRepository.save(new Atributo(new AtributoId(), "Resistência", null));
        testRegraDistribuicao = regraDistribuicaoRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), atividade, atributo, 0.8));
        testFatorCalculo = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Distância", "km", TipoInput.NUMERICO));
    }

    @Test
    void create_withValidData_shouldReturn201() throws Exception {
        CreateRegraFatorXPRequest request = new CreateRegraFatorXPRequest(testFatorCalculo.getId().getValue(), 1.5, 5.0, 21.0);

        mockMvc.perform(post("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp", testRegraDistribuicao.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fatorCalculoNome").value("Distância"))
                .andExpect(jsonPath("$.pesoMultiplicador").value(1.5));
    }

    @Test
    void create_whenRegraDistribuicaoNotFound_shouldReturn404() throws Exception {
        CreateRegraFatorXPRequest request = new CreateRegraFatorXPRequest(testFatorCalculo.getId().getValue(), 1.5, 5.0, 21.0);

        mockMvc.perform(post("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_withValidData_shouldReturn200() throws Exception {
        RegraFatorXP regra = regraFatorXPRepository.save(new RegraFatorXP(new RegraFatorXPId(), testRegraDistribuicao, testFatorCalculo, 1.0, 1.0, 10.0));
        UpdateRegraFatorXPRequest request = new UpdateRegraFatorXPRequest(testFatorCalculo.getId().getValue(), 2.0, 2.0, 12.0);

        mockMvc.perform(put("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp/{regraFatorXPId}", testRegraDistribuicao.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pesoMultiplicador").value(2.0))
                .andExpect(jsonPath("$.pontoCorteMax").value(12.0));
    }

    @Test
    void update_whenRegraFatorXPNotFound_shouldReturn404() throws Exception {
        UpdateRegraFatorXPRequest request = new UpdateRegraFatorXPRequest(testFatorCalculo.getId().getValue(), 2.0, 2.0, 12.0);

        mockMvc.perform(put("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp/{regraFatorXPId}", testRegraDistribuicao.getId().getValue(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_whenExists_shouldReturn204() throws Exception {
        RegraFatorXP regra = regraFatorXPRepository.save(new RegraFatorXP(new RegraFatorXPId(), testRegraDistribuicao, testFatorCalculo, 1.0, 1.0, 10.0));

        mockMvc.perform(delete("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp/{regraFatorXPId}", testRegraDistribuicao.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp/{regraFatorXPId}", testRegraDistribuicao.getId().getValue(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }
}
