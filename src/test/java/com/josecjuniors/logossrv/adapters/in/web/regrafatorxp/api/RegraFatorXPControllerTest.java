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
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormularioId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.application.service.AtividadeFormularioService;
import com.josecjuniors.logossrv.core.atividadeformulario.application.port.in.ReplaceAtividadeFormularioCommand;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.UpdateRegraFatorXPCommand;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.UpdateRegraFatorXPUseCase;
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
    private AtividadeFormularioRepository atividadeFormularioRepository;
    @Autowired
    private AtividadeFormularioService atividadeFormularioService;
    @Autowired
    private UpdateRegraFatorXPUseCase updateRegraFatorXPUseCase;
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
        atividadeFormularioRepository.deleteAll();
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
                .andExpect(status().isGone());
    }

    @Test
    void create_whenRegraDistribuicaoNotFound_shouldReturn404() throws Exception {
        CreateRegraFatorXPRequest request = new CreateRegraFatorXPRequest(testFatorCalculo.getId().getValue(), 1.5, 5.0, 21.0);

        mockMvc.perform(post("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }

    @Test
    void update_withValidData_shouldReturn200() throws Exception {
        RegraFatorXP regra = regraFatorXPRepository.save(new RegraFatorXP(new RegraFatorXPId(), testRegraDistribuicao, testFatorCalculo, 1.0, 1.0, 10.0));
        UpdateRegraFatorXPRequest request = new UpdateRegraFatorXPRequest(testFatorCalculo.getId().getValue(), 2.0, 2.0, 12.0);

        mockMvc.perform(put("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp/{regraFatorXPId}", testRegraDistribuicao.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }

    @Test
    void updatingLegacyXpRuleDoesNotChangeAuthoredActivityForm() {
        RegraFatorXP regra = regraFatorXPRepository.save(new RegraFatorXP(new RegraFatorXPId(), testRegraDistribuicao, testFatorCalculo, 1.0, 1.0, 10.0));
        atividadeFormularioService.replace(new ReplaceAtividadeFormularioCommand(testRegraDistribuicao.getAtividadeConfig().getId(), 0,
                java.util.List.of(new ReplaceAtividadeFormularioCommand.Campo(testFatorCalculo.getId().getValue(), "Distância em km"))));
        AtividadeFormulario before = atividadeFormularioRepository.findByAtividadeConfigId(testRegraDistribuicao.getAtividadeConfig().getId()).orElseThrow();
        int version = before.getVersao();
        String placeholder = before.getFormularioJson().campos().get(0).placeholder();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> updateRegraFatorXPUseCase.update(new UpdateRegraFatorXPCommand(regra.getId(), testFatorCalculo.getId(), 2.0, 20.0, 30.0)))
                .isInstanceOf(com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException.class);

        AtividadeFormulario after = atividadeFormularioRepository.findByAtividadeConfigId(testRegraDistribuicao.getAtividadeConfig().getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(after.getVersao()).isEqualTo(version);
        org.assertj.core.api.Assertions.assertThat(after.getFormularioJson().campos()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(after.getFormularioJson().campos().get(0).placeholder()).isEqualTo(placeholder);
    }

    @Test
    void update_whenRegraFatorXPNotFound_shouldReturn404() throws Exception {
        UpdateRegraFatorXPRequest request = new UpdateRegraFatorXPRequest(testFatorCalculo.getId().getValue(), 2.0, 2.0, 12.0);

        mockMvc.perform(put("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp/{regraFatorXPId}", testRegraDistribuicao.getId().getValue(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }

    @Test
    void delete_whenExists_shouldReturn204() throws Exception {
        RegraFatorXP regra = regraFatorXPRepository.save(new RegraFatorXP(new RegraFatorXPId(), testRegraDistribuicao, testFatorCalculo, 1.0, 1.0, 10.0));

        mockMvc.perform(delete("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp/{regraFatorXPId}", testRegraDistribuicao.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isGone());
    }

    @Test
    void delete_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp/{regraFatorXPId}", testRegraDistribuicao.getId().getValue(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isGone());
    }
}
