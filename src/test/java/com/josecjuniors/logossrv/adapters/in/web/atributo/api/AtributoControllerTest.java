package com.josecjuniors.logossrv.adapters.in.web.atributo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.atributo.dto.request.CreateAtributoRequest;
import com.josecjuniors.logossrv.adapters.in.web.atributo.dto.request.UpdateAtributoRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class AtributoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        appUserRepository.deleteAll();
        AppUser testAppUser = new AppUser(new AppUserId(), "test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);
    }

    // --- CREATE Tests ---
    @Test
    void createAtributo_shouldReturn201AndCreatedAtributo() throws Exception {
        CreateAtributoRequest request = new CreateAtributoRequest("Foco", null);

        mockMvc.perform(post("/api/atributos")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Foco"));
    }

    @Test
    void createAtributo_whenNameAlreadyExists_shouldReturn409Conflict() throws Exception {
        atributoRepository.save(new Atributo(new AtributoId(), "Agilidade", null));
        CreateAtributoRequest request = new CreateAtributoRequest("Agilidade", "Capacidade de se mover rapidamente.");

        mockMvc.perform(post("/api/atributos")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // --- READ Tests ---
    @Test
    void getAtributoById_whenAtributoExists_shouldReturn200AndAtributo() throws Exception {
        Atributo atributo = new Atributo(new AtributoId(), "Inteligência", null);
        Atributo savedAtributo = atributoRepository.save(atributo);
        UUID atributoId = savedAtributo.getId().getValue();

        mockMvc.perform(get("/api/atributos/{id}", atributoId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(atributoId.toString()))
                .andExpect(jsonPath("$.nome").value("Inteligência"));
    }

    @Test
    void getAtributoById_whenAtributoDoesNotExist_shouldReturn404NotFound() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/atributos/{id}", randomId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    // --- UPDATE Tests ---
    @Test
    void updateAtributo_whenAtributoExists_shouldReturn200AndUpdatedAtributo() throws Exception {
        Atributo atributo = new Atributo(new AtributoId(), "Forca", "");
        Atributo savedAtributo = atributoRepository.save(atributo);
        UUID atributoId = savedAtributo.getId().getValue();

        UpdateAtributoRequest request = new UpdateAtributoRequest("Força", "Força é a tensão gerada por um músculo ou grupo muscular contra uma resistência.");

        mockMvc.perform(put("/api/atributos/{id}", atributoId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(atributoId.toString()))
                .andExpect(jsonPath("$.nome").value("Força"));
    }

    @Test
    void updateAtributo_whenAtributoDoesNotExist_shouldReturn404NotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        UpdateAtributoRequest request = new UpdateAtributoRequest("Nome Inexistente", null);

        mockMvc.perform(put("/api/atributos/{id}", randomId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAtributo_whenNameAlreadyExistsInAnotherAtributo_shouldReturn409Conflict() throws Exception {
        atributoRepository.save(new Atributo(new AtributoId(), "Destreza", "A capacidade de realizar movimentos rápidos, precisos e eficazes, especialmente com as mãos."));
        Atributo atributoParaAtualizar = atributoRepository.save(new Atributo(new AtributoId(), "Sorte", "certas características e comportamentos que aumentam a probabilidade de resultados favoráveis."));
        UUID idParaAtualizar = atributoParaAtualizar.getId().getValue();

        UpdateAtributoRequest request = new UpdateAtributoRequest("Destreza", "A capacidade de realizar movimentos rápidos, precisos e eficazes");

        mockMvc.perform(put("/api/atributos/{id}", idParaAtualizar)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // --- GET ALL (Paginated) Tests ---
    @Test
    void getAllAtributos_shouldReturnPaginatedResult() throws Exception {
        atributoRepository.saveAll(List.of(
                new Atributo(new AtributoId(), "Força Bruta", null),
                new Atributo(new AtributoId(), "Força de Vontade", null),
                new Atributo(new AtributoId(), "Super Força", null),
                new Atributo(new AtributoId(), "Agilidade",null)
        ));

        mockMvc.perform(get("/api/atributos")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("searchTerm", "Força")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void getAllAtributos_shouldReturnSortedResult() throws Exception {
        atributoRepository.saveAll(List.of(
                new Atributo(new AtributoId(), "Coragem", null),
                new Atributo(new AtributoId(), "Agilidade", null),
                new Atributo(new AtributoId(), "Destreza", null)
        ));

        mockMvc.perform(get("/api/atributos")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("sort", "nome,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Agilidade"));
    }
}
