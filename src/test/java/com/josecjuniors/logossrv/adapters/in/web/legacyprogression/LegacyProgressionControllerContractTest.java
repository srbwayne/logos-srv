package com.josecjuniors.logossrv.adapters.in.web.legacyprogression;

import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class LegacyProgressionControllerContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppUserJpaRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        var user = users.save(new AppUser(new AppUserId(), "legacy-api.test@email.com", passwordEncoder.encode("password")));
        token = jwtService.generateToken(user);
    }

    @Test
    void distributionMutationRoutesRemain410AndGetRemainsUnmapped() throws Exception {
        UUID activityId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        String base = "/api/atividades-config/" + activityId + "/regras-distribuicao";

        mockMvc.perform(post(base)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"atributoId\":\"" + UUID.randomUUID() + "\",\"pesoPercentual\":0.5}"))
                .andExpect(status().isGone());
        mockMvc.perform(put(base + "/" + ruleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoPercentual\":0.5}"))
                .andExpect(status().isGone());
        mockMvc.perform(delete(base + "/" + ruleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isGone());
        mockMvc.perform(get(base).header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void xpMutationRoutesRemain410WithoutPersistenceLookup() throws Exception {
        UUID distributionId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        String base = "/api/regras-distribuicao/" + distributionId + "/fatores-xp";
        String body = "{\"fatorCalculoId\":\"" + UUID.randomUUID() + "\",\"pesoMultiplicador\":1.0,\"pontoCorteMin\":0,\"pontoCorteMax\":10}";

        mockMvc.perform(post(base).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isGone());
        mockMvc.perform(put(base + "/" + ruleId).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isGone());
        mockMvc.perform(delete(base + "/" + ruleId).header("Authorization", "Bearer " + token)).andExpect(status().isGone());
    }

    @Test
    void stressMutationRoutesRemain410WithoutPersistenceLookup() throws Exception {
        UUID distributionId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        String base = "/api/regras-distribuicao/" + distributionId + "/fatores-estresse";
        String body = "{\"pesoMultiplicador\":1.0,\"pontoCorteMin\":0,\"pontoCorteMax\":10,\"tipo\":\"POSITIVO\"}";

        mockMvc.perform(post(base).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isGone());
        mockMvc.perform(put(base + "/" + ruleId).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isGone());
        mockMvc.perform(delete(base + "/" + ruleId).header("Authorization", "Bearer " + token)).andExpect(status().isGone());
    }
}
