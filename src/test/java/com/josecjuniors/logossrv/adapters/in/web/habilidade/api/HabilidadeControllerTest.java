package com.josecjuniors.logossrv.adapters.in.web.habilidade.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.CreateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.UpdateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisito;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisitoId;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRequisitoRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private AtributoRepository atributoRepository;
    @Autowired
    private HabilidadeRequisitoRepository habilidadeRequisitoRepository;
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
        CreateHabilidadeRequest request = new CreateHabilidadeRequest("Natação", "Descrição qualquer");
        mockMvc.perform(post("/api/habilidades")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateHabilidade_whenHabilidadeExists_shouldReturn200AndUpdatedHabilidade() throws Exception {
        Habilidade habilidade = habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Corrida", "Descrição antiga"));
        UpdateHabilidadeRequest request = new UpdateHabilidadeRequest("Corrida de Fundo", "Nova descrição");
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

    @Test
    void getRequisitos_whenHabilidadeHasRequisitos_shouldReturn200AndListOfRequisitos() throws Exception {
        Habilidade artesanatoBasico = habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Artesanato Básico", null));
        Atributo destreza = atributoRepository.save(new Atributo(new AtributoId(), "Destreza", ""));
        Habilidade forjaDeArmas = habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Forja de Armas", null));
        HabilidadeRequisito req1 = HabilidadeRequisito.paraHabilidade(new HabilidadeRequisitoId(), forjaDeArmas, artesanatoBasico, 5);
        HabilidadeRequisito req2 = HabilidadeRequisito.paraAtributo(new HabilidadeRequisitoId(), forjaDeArmas, destreza, 10);
        habilidadeRequisitoRepository.save(req1);
        habilidadeRequisitoRepository.save(req2);
        mockMvc.perform(get("/api/habilidades/{habilidadeId}/requisitos", forjaDeArmas.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nomeRequisito", containsInAnyOrder("Artesanato Básico", "Destreza")));
    }

    @Test
    void getRequisitos_whenHabilidadeHasNoRequisitos_shouldReturn200AndEmptyList() throws Exception {
        Habilidade habilidadeSimples = habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Caminhada", null));
        mockMvc.perform(get("/api/habilidades/{habilidadeId}/requisitos", habilidadeSimples.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getRequisitos_withoutToken_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/habilidades/{habilidadeId}/requisitos", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRequisitoById_whenRequisitoExists_shouldReturn200AndRequisitoDetails() throws Exception {
        Habilidade forjaDeArmas = habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Forja de Armas", null));
        Atributo forca = atributoRepository.save(new Atributo(new AtributoId(), "Força", ""));
        HabilidadeRequisito requisito = habilidadeRequisitoRepository.save(HabilidadeRequisito.paraAtributo(new HabilidadeRequisitoId(), forjaDeArmas, forca, 15));

        mockMvc.perform(get("/api/habilidades/{habilidadeId}/requisitos/{requisitoId}", forjaDeArmas.getId().getValue(), requisito.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requisito.getId().getValue().toString()))
                .andExpect(jsonPath("$.tipo").value("ATRIBUTO"))
                .andExpect(jsonPath("$.requisitoId").value(forca.getId().getValue().toString()))
                .andExpect(jsonPath("$.requisitoNome").value("Força"))
                .andExpect(jsonPath("$.nivelMinimo").value(15));
    }

    @Test
    void getRequisitoById_whenRequisitoDoesNotExist_shouldReturn404NotFound() throws Exception {
        Habilidade habilidade = habilidadeRepository.save(new Habilidade(new HabilidadeId(), "Qualquer Habilidade", null));
        mockMvc.perform(get("/api/habilidades/{habilidadeId}/requisitos/{requisitoId}", habilidade.getId().getValue(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }
}
