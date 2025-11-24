package com.josecjuniors.logossrv.adapters.in.web.debuff.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.debuff.dto.request.CreateDebuffRequest;
import com.josecjuniors.logossrv.adapters.in.web.debuff.dto.request.CreateRegraDistribuicaoDebuffRequest;
import com.josecjuniors.logossrv.adapters.in.web.debuff.dto.request.UpdateDebuffRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import com.josecjuniors.logossrv.core.debuff.domain.repository.RegraDistribuicaoDebuffRepository;
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
class DebuffControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DebuffRepository debuffRepository;
    @Autowired
    private RegraDistribuicaoDebuffRepository regraRepository;
    @Autowired
    private AtributoRepository atributoRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        regraRepository.deleteAll();
        debuffRepository.deleteAll();
        atributoRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser testAppUser = new AppUser(new AppUserId(), "debuff.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);
    }

    // --- Debuff CRUD Tests ---

    @Test
    void createDebuff_withValidData_shouldReturn201() throws Exception {
        var request = new CreateDebuffRequest("Ressaca");

        mockMvc.perform(post("/api/debuffs")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Ressaca"));
    }

    @Test
    void createDebuff_whenNameExists_shouldReturn409() throws Exception {
        debuffRepository.save(new Debuff(DebuffId.generate(), "Ressaca"));
        var request = new CreateDebuffRequest("Ressaca");

        mockMvc.perform(post("/api/debuffs")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllDebuffs_shouldReturnPagedResult() throws Exception {
        debuffRepository.save(new Debuff(DebuffId.generate(), "Fadiga"));

        mockMvc.perform(get("/api/debuffs")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nome").value("Fadiga"));
    }

    @Test
    void getDebuffById_whenExists_shouldReturn200() throws Exception {
        var debuff = debuffRepository.save(new Debuff(DebuffId.generate(), "Lesão"));

        mockMvc.perform(get("/api/debuffs/{id}", debuff.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Lesão"));
    }

    @Test
    void updateDebuff_withValidData_shouldReturn200() throws Exception {
        var debuff = debuffRepository.save(new Debuff(DebuffId.generate(), "Nome Antigo"));
        var request = new UpdateDebuffRequest("Nome Novo");

        mockMvc.perform(put("/api/debuffs/{id}", debuff.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"));
    }

    // --- RegraDistribuicaoDebuff CRUD Tests ---

    @Test
    void createRegra_withValidData_shouldReturn201() throws Exception {
        var debuff = debuffRepository.save(new Debuff(DebuffId.generate(), "Névoa Mental"));
        var atributo = atributoRepository.save(new Atributo(new AtributoId(), "Inteligência", "Capcidade de abstração e resolução de problemas logicos"));
        var request = new CreateRegraDistribuicaoDebuffRequest(atributo.getId().getValue());

        mockMvc.perform(post("/api/debuffs/{debuffId}/regras-distribuicao", debuff.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.debuffNome").value("Névoa Mental"))
                .andExpect(jsonPath("$.atributoNome").value("Inteligência"));
    }

    @Test
    void getAllRegras_whenExists_shouldReturnPagedResult() throws Exception {
        var debuff = debuffRepository.save(new Debuff(DebuffId.generate(), "Fraqueza"));
        var atributo = atributoRepository.save(new Atributo(new AtributoId(), "Força", "Atrubuto referente a capcidade de força fisica"));
        regraRepository.save(new RegraDistribuicaoDebuff(RegraDistribuicaoDebuffId.generate(), debuff, atributo));

        mockMvc.perform(get("/api/debuffs/{debuffId}/regras-distribuicao", debuff.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].atributoNome").value("Força"));
    }

    @Test
    void deleteRegra_whenExists_shouldReturn204() throws Exception {
        var debuff = debuffRepository.save(new Debuff(DebuffId.generate(), "Desmotivação"));
        var atributo = atributoRepository.save(new Atributo(new AtributoId(), "Disciplina", "Capacidade de manter constancia em um objetivo"));
        var regra = regraRepository.save(new RegraDistribuicaoDebuff(RegraDistribuicaoDebuffId.generate(), debuff, atributo));

        mockMvc.perform(delete("/api/debuffs/{debuffId}/regras-distribuicao/{regraId}", debuff.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        assertThat(regraRepository.findById(regra.getId())).isEmpty();
    }

    @Test
    void deleteRegra_whenRegraDoesNotBelongToDebuff_shouldReturn403() throws Exception {
        var debuff1 = debuffRepository.save(new Debuff(DebuffId.generate(), "Debuff A"));
        var debuff2 = debuffRepository.save(new Debuff(DebuffId.generate(), "Debuff B"));
        var atributo = atributoRepository.save(new Atributo(AtributoId.generate(), "Atributo", "Descrição Atributo"));
        var regra = regraRepository.save(new RegraDistribuicaoDebuff(RegraDistribuicaoDebuffId.generate(), debuff1, atributo));

        mockMvc.perform(delete("/api/debuffs/{debuffId}/regras-distribuicao/{regraId}", debuff2.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }
}
