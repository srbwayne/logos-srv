package com.josecjuniors.logossrv.adapters.in.web.habilidade.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.CreateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.UpdateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class HabilidadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HabilidadeRepository habilidadeRepository;

    @Autowired
    private AppUserJpaRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();
        AppUser testAppUser = new AppUser(new AppUserId(), "habilidade.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);
    }

    @Test
    void createHabilidade_whenNameAlreadyExists_shouldReturn409Conflict() throws Exception {
        habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Natação", null));
        CreateHabilidadeRequest request = new CreateHabilidadeRequest("Natação", null);

        mockMvc.perform(post("/api/habilidades")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateHabilidade_whenHabilidadeExists_shouldReturn200AndUpdatedHabilidade() throws Exception {
        Habilidade habilidade = habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Corrida", null));
        UpdateHabilidadeRequest request = new UpdateHabilidadeRequest("Corrida de Fundo", null);

        mockMvc.perform(put("/api/habilidades/{id}", habilidade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Corrida de Fundo"));
    }

    @Test
    void updateHabilidade_whenHabilidadeDoesNotExist_shouldReturn404NotFound() throws Exception {
        UpdateHabilidadeRequest request = new UpdateHabilidadeRequest("Inexistente", null);

        mockMvc.perform(put("/api/habilidades/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateHabilidade_whenNameAlreadyExistsInAnotherHabilidade_shouldReturn409Conflict() throws Exception {
        habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Ciclismo", null));
        Habilidade habilidadeParaAtualizar = habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Musculação", null));
        UpdateHabilidadeRequest request = new UpdateHabilidadeRequest("Ciclismo", null);

        mockMvc.perform(put("/api/habilidades/{id}", habilidadeParaAtualizar.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
