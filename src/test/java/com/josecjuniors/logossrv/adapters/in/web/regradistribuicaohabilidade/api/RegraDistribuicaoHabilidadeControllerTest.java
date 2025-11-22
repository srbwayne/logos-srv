package com.josecjuniors.logossrv.adapters.in.web.regradistribuicaohabilidade.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaohabilidade.dto.request.CreateRegraDistribuicaoHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaohabilidade.dto.request.UpdateRegraDistribuicaoHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidade;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.repository.RegraDistribuicaoHabilidadeRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class RegraDistribuicaoHabilidadeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegraDistribuicaoHabilidadeRepository repository;
    @Autowired
    private HabilidadeRepository habilidadeRepository;
    @Autowired
    private AtributoRepository atributoRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private Habilidade habilidade;
    private Atributo atributo;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        habilidadeRepository.deleteAll();
        atributoRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser testAppUser = new AppUser(new AppUserId(), "regradist.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);

        habilidade = habilidadeRepository.save(new Habilidade(HabilidadeId.generate(), "Persuasão", null));
        atributo = atributoRepository.save(new Atributo(new AtributoId(), "Carisma", null));
    }

    @Test
    void create_withValidData_shouldReturn201() throws Exception {
        var request = new CreateRegraDistribuicaoHabilidadeRequest(atributo.getId().getValue(), 0.8);

        mockMvc.perform(post("/api/habilidades/{habilidadeId}/regras-distribuicao", habilidade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atributoNome").value("Carisma"))
                .andExpect(jsonPath("$.pesoDistribuicao").value(0.8));
    }

    @Test
    void getAll_whenExists_shouldReturn200AndList() throws Exception {
        repository.save(new RegraDistribuicaoHabilidade(RegraDistribuicaoHabilidadeId.generate(), habilidade, atributo, 0.5));

        mockMvc.perform(get("/api/habilidades/{habilidadeId}/regras-distribuicao", habilidade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].atributoNome").value("Carisma"));
    }

    @Test
    void update_withValidData_shouldReturn200() throws Exception {
        var regra = repository.save(new RegraDistribuicaoHabilidade(RegraDistribuicaoHabilidadeId.generate(), habilidade, atributo, 0.5));
        var request = new UpdateRegraDistribuicaoHabilidadeRequest(0.9);

        mockMvc.perform(put("/api/habilidades/{habilidadeId}/regras-distribuicao/{regraId}", habilidade.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pesoDistribuicao").value(0.9));
    }

    @Test
    void update_whenRegraDoesNotBelongToHabilidade_shouldReturn403() throws Exception {
        var outraHabilidade = habilidadeRepository.save(new Habilidade(HabilidadeId.generate(), "Intimidação", null));

        var regra = repository.save(new RegraDistribuicaoHabilidade(RegraDistribuicaoHabilidadeId.generate(), habilidade, atributo, 0.5));
        var request = new UpdateRegraDistribuicaoHabilidadeRequest(0.9);

        mockMvc.perform(put("/api/habilidades/{habilidadeId}/regras-distribuicao/{regraId}", outraHabilidade.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_whenExists_shouldReturn204() throws Exception {
        var regra = repository.save(new RegraDistribuicaoHabilidade(RegraDistribuicaoHabilidadeId.generate(), habilidade, atributo, 0.5));

        mockMvc.perform(delete("/api/habilidades/{habilidadeId}/regras-distribuicao/{regraId}", habilidade.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        assertThat(repository.findById(regra.getId())).isEmpty();
    }

    @Test
    void delete_whenRegraDoesNotBelongToHabilidade_shouldReturn403() throws Exception {
        var outraHabilidade = habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Intimidação", null));
        var regra = repository.save(new RegraDistribuicaoHabilidade(RegraDistribuicaoHabilidadeId.generate(), habilidade, atributo, 0.5));

        mockMvc.perform(delete("/api/habilidades/{habilidadeId}/regras-distribuicao/{regraId}", outraHabilidade.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutToken_shouldReturn403() throws Exception {
        var request = new CreateRegraDistribuicaoHabilidadeRequest(atributo.getId().getValue(), 0.8);

        mockMvc.perform(post("/api/habilidades/{habilidadeId}/regras-distribuicao", habilidade.getId().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
