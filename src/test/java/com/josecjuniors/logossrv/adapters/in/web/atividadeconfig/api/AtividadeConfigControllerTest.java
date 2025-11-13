package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.CreateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.UpdateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
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
class AtividadeConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AtividadeConfigRepository atividadeConfigRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        atividadeConfigRepository.deleteAll();
        appUserRepository.deleteAll();
        AppUser testAppUser = new AppUser(new AppUserId(), "atividade.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);
    }

    @Test
    void create_withValidData_shouldReturn201() throws Exception {
        CreateAtividadeConfigRequest request = new CreateAtividadeConfigRequest("Corrida", "Corrida ao ar livre", 100, 10, null, null);

        mockMvc.perform(post("/api/atividades-config")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Corrida"));
    }

    @Test
    void create_whenNameIsTaken_shouldReturn409() throws Exception {
        atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", null, 50, -5, null, null));
        CreateAtividadeConfigRequest request = new CreateAtividadeConfigRequest("Leitura", null, 60, -10, null, null);

        mockMvc.perform(post("/api/atividades-config")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getById_whenExists_shouldReturn200() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Estudar", null, 80, 5, null, null));

        mockMvc.perform(get("/api/atividades-config/{id}", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(atividade.getId().getValue().toString()));
    }

    @Test
    void getById_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/atividades-config/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_withSearchTerm_shouldReturnFilteredPage() throws Exception {
        atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida Leve", null, 100, 10, null, null));
        atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida Longa", null, 300, 25, null, null));
        atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Natação", null, 150, 5, null, null));

        mockMvc.perform(get("/api/atividades-config")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("searchTerm", "Corrida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void update_withValidData_shouldReturn200() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Musculação", null, 120, 15, null, null));
        UpdateAtividadeConfigRequest request = new UpdateAtividadeConfigRequest("Treino de Força", "Foco em hipertrofia", 150, 20, null, null);

        mockMvc.perform(put("/api/atividades-config/{id}", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Treino de Força"))
                .andExpect(jsonPath("$.descricao").value("Foco em hipertrofia"));
    }

    @Test
    void update_whenNameIsTaken_shouldReturn409() throws Exception {
        atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Ciclismo", null, 200, 10, null, null));
        AtividadeConfig atividadeToUpdate = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Yoga", null, 40, -15, null, null));
        UpdateAtividadeConfigRequest request = new UpdateAtividadeConfigRequest("Ciclismo", null, 0, 0, null, null);

        mockMvc.perform(put("/api/atividades-config/{id}", atividadeToUpdate.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_whenExists_shouldReturn204() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Meditação", null, 20, -20, null, null));

        mockMvc.perform(delete("/api/atividades-config/{id}", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }
}
