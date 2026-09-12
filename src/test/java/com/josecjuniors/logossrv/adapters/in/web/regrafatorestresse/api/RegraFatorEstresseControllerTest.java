package com.josecjuniors.logossrv.adapters.in.web.regrafatorestresse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.regrafatorestresse.dto.request.CreateRegraFatorEstresseRequest;
import com.josecjuniors.logossrv.adapters.in.web.regrafatorestresse.dto.request.UpdateRegraFatorEstresseRequest;
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
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.repository.RegraFatorEstresseRepository;
import com.josecjuniors.logossrv.adapters.out.regrafatorestresse.jpa.RegraFatorEstresseJpaRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class RegraFatorEstresseControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegraFatorEstresseRepository regraFatorEstresseRepository;
    @Autowired
    private RegraFatorEstresseJpaRepository regraFatorEstressePersistence;
    @Autowired
    private RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository;
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
    private RegraDistribuicaoAtividade testRegraDistribuicao;

    @BeforeEach
    void setUp() {
        regraFatorEstresseRepository.deleteAll();
        regraDistribuicaoRepository.deleteAll();
        atividadeConfigRepository.deleteAll();
        atributoRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser testAppUser = new AppUser(new AppUserId(), "regra-estresse.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);

        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Discussão", null, 0, 0, null, null));
        Atributo atributo = atributoRepository.save(new Atributo(new AtributoId(), "Paciência", null));
        testRegraDistribuicao = regraDistribuicaoRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), atividade, atributo, 0.5));
    }

    @Test
    void create_withValidData_shouldReturn201() throws Exception {
        CreateRegraFatorEstresseRequest request = new CreateRegraFatorEstresseRequest(1.2, 30.0, 60.0, TipoFatorEstresse.POSITIVO);

        mockMvc.perform(post("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-estresse", testRegraDistribuicao.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
        org.assertj.core.api.Assertions.assertThat(regraFatorEstressePersistence.count()).isZero();
    }

    @Test
    void create_whenRegraDistribuicaoNotFound_shouldReturn404() throws Exception {
        CreateRegraFatorEstresseRequest request = new CreateRegraFatorEstresseRequest(1.2, 30.0, 60.0, TipoFatorEstresse.POSITIVO);

        mockMvc.perform(post("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-estresse", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }

    @Test
    void update_withValidData_shouldReturn200() throws Exception {
        RegraFatorEstresse regra = regraFatorEstresseRepository.save(new RegraFatorEstresse(new RegraFatorEstresseId(), testRegraDistribuicao, 1.0, 10.0, 20.0, TipoFatorEstresse.POSITIVO));
        UpdateRegraFatorEstresseRequest request = new UpdateRegraFatorEstresseRequest(0.8, 15.0, 25.0, TipoFatorEstresse.NEGATIVO);

        mockMvc.perform(put("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-estresse/{regraFatorEstresseId}", testRegraDistribuicao.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
        RegraFatorEstresse persisted = regraFatorEstresseRepository.findById(regra.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(persisted.getPesoMultiplicador()).isEqualTo(1.0);
        org.assertj.core.api.Assertions.assertThat(persisted.getPontoCorteMin()).isEqualTo(10.0);
        org.assertj.core.api.Assertions.assertThat(persisted.getPontoCorteMax()).isEqualTo(20.0);
        org.assertj.core.api.Assertions.assertThat(persisted.getTipo()).isEqualTo(TipoFatorEstresse.POSITIVO);
    }

    @Test
    void update_whenRegraFatorEstresseNotFound_shouldReturn404() throws Exception {
        UpdateRegraFatorEstresseRequest request = new UpdateRegraFatorEstresseRequest(0.8, 15.0, 25.0, TipoFatorEstresse.NEGATIVO);

        mockMvc.perform(put("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-estresse/{regraFatorEstresseId}", testRegraDistribuicao.getId().getValue(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }

    @Test
    void delete_whenExists_shouldReturn204() throws Exception {
        RegraFatorEstresse regra = regraFatorEstresseRepository.save(new RegraFatorEstresse(new RegraFatorEstresseId(), testRegraDistribuicao, 1.0, 10.0, 20.0, TipoFatorEstresse.POSITIVO));

        mockMvc.perform(delete("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-estresse/{regraFatorEstresseId}", testRegraDistribuicao.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isGone());
        org.assertj.core.api.Assertions.assertThat(regraFatorEstresseRepository.findById(regra.getId())).isPresent();
    }

    @Test
    void delete_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-estresse/{regraFatorEstresseId}", testRegraDistribuicao.getId().getValue(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isGone());
    }

    @Test
    void create_withoutToken_shouldReturn403() throws Exception {
        CreateRegraFatorEstresseRequest request = new CreateRegraFatorEstresseRequest(1.2, 30.0, 60.0, TipoFatorEstresse.POSITIVO);

        mockMvc.perform(post("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-estresse", testRegraDistribuicao.getId().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
