package com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.dto.request.CreateFatorCalculoRequest;
import com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.dto.request.UpdateFatorCalculoRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
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
class FatorCalculoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FatorCalculoRepository fatorCalculoRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        fatorCalculoRepository.deleteAll();
        appUserRepository.deleteAll();
        AppUser testAppUser = new AppUser(new AppUserId(), "fator.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);
    }

    @Test
    void create_withValidData_shouldReturn201AndCreatedFator() throws Exception {
        CreateFatorCalculoRequest request = new CreateFatorCalculoRequest("Distância", "km", TipoInput.NUMERICO);

        mockMvc.perform(post("/api/fatores-calculo")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Distância"))
                .andExpect(jsonPath("$.unidadeMedida").value("km"))
                .andExpect(jsonPath("$.tipoInput").value("NUMERICO"));
    }

    @Test
    void create_whenNameIsTaken_shouldReturn409Conflict() throws Exception {
        fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Tempo", "min", TipoInput.NUMERICO));
        CreateFatorCalculoRequest request = new CreateFatorCalculoRequest("Tempo", "h", TipoInput.NUMERICO);

        mockMvc.perform(post("/api/fatores-calculo")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getById_whenFatorExists_shouldReturn200AndFator() throws Exception {
        FatorCalculo fator = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Páginas Lidas", "páginas", TipoInput.NUMERICO));

        mockMvc.perform(get("/api/fatores-calculo/{id}", fator.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fator.getId().getValue().toString()))
                .andExpect(jsonPath("$.nome").value("Páginas Lidas"));
    }

    @Test
    void getById_whenFatorDoesNotExist_shouldReturn404NotFound() throws Exception {
        mockMvc.perform(get("/api/fatores-calculo/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_shouldReturnPagedFatores() throws Exception {
        fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Distância", "km", TipoInput.NUMERICO));
        fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Tempo", "min", TipoInput.NUMERICO));

        mockMvc.perform(get("/api/fatores-calculo")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("sort", "nome,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].nome").value("Distância"));
    }

    @Test
    void update_withValidData_shouldReturn200AndUpdatedFator() throws Exception {
        FatorCalculo fator = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Peso", "kg", TipoInput.NUMERICO));
        UpdateFatorCalculoRequest request = new UpdateFatorCalculoRequest("Peso Corporal", "kg", TipoInput.NUMERICO);

        mockMvc.perform(put("/api/fatores-calculo/{id}", fator.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Peso Corporal"));
    }

    @Test
    void update_whenNameIsTaken_shouldReturn409Conflict() throws Exception {
        fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Calorias", "kcal", TipoInput.NUMERICO));
        FatorCalculo fatorToUpdate = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Passos", "passos", TipoInput.NUMERICO));
        UpdateFatorCalculoRequest request = new UpdateFatorCalculoRequest("Calorias", "kcal", TipoInput.NUMERICO);

        mockMvc.perform(put("/api/fatores-calculo/{id}", fatorToUpdate.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_whenFatorExists_shouldReturn204() throws Exception {
        FatorCalculo fator = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "A ser deletado", "un", TipoInput.NUMERICO));

        mockMvc.perform(delete("/api/fatores-calculo/{id}", fator.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }
}
