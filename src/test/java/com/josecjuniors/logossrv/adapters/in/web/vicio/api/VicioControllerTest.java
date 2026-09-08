package com.josecjuniors.logossrv.adapters.in.web.vicio.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.vicio.dto.request.CreateRegraVicioRequest;
import com.josecjuniors.logossrv.adapters.in.web.vicio.dto.request.CreateVicioRequest;
import com.josecjuniors.logossrv.adapters.in.web.vicio.dto.request.UpdateVicioRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicioId;
import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import com.josecjuniors.logossrv.core.vicio.domain.repository.RegraVicioRepository;
import com.josecjuniors.logossrv.core.vicio.domain.repository.VicioRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class VicioControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private VicioRepository vicioRepository;
    @Autowired
    private RegraVicioRepository regraVicioRepository;
    @Autowired
    private DebuffRepository debuffRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        regraVicioRepository.deleteAll();
        vicioRepository.deleteAll();
        debuffRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser testAppUser = new AppUser(new AppUserId(), "vicio.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);
    }

    // --- Vicio CRUD Tests ---

    @Test
    void createVicio_withValidData_shouldReturn201() throws Exception {
        var request = new CreateVicioRequest("Procrastinação", "Deixar para depois");

        mockMvc.perform(post("/api/vicios")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Procrastinação"));
    }

    @Test
    void createVicio_whenNameExists_shouldReturn409() throws Exception {
        vicioRepository.save(new Vicio(VicioId.generate(), "Procrastinação", null));
        var request = new CreateVicioRequest("Procrastinação", null);

        mockMvc.perform(post("/api/vicios")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllVicios_shouldReturnPagedResult() throws Exception {
        vicioRepository.save(new Vicio(VicioId.generate(), "Fumar", null));

        mockMvc.perform(get("/api/vicios")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nome").value("Fumar"));
    }

    @Test
    void getVicioById_whenExists_shouldReturn200() throws Exception {
        var vicio = vicioRepository.save(new Vicio(VicioId.generate(), "Beber Café", null));

        mockMvc.perform(get("/api/vicios/{id}", vicio.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Beber Café"));
    }

    @Test
    void updateVicio_withValidData_shouldReturn200() throws Exception {
        var vicio = vicioRepository.save(new Vicio(VicioId.generate(), "Nome Antigo", null));
        var request = new UpdateVicioRequest("Nome Novo", "Desc Nova");

        mockMvc.perform(put("/api/vicios/{id}", vicio.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"))
                .andExpect(jsonPath("$.descricao").value("Desc Nova"));
    }

    // --- RegraVicio CRUD Tests ---

    @Test
    void createRegra_withValidData_shouldReturn201() throws Exception {
        var vicio = vicioRepository.save(new Vicio(VicioId.generate(), "Alcoolismo", null));
        var debuff = debuffRepository.save(new Debuff(DebuffId.generate(), "Ressaca"));
        var request = new CreateRegraVicioRequest(10, -50, 8, 8, debuff.getId().getValue());

        mockMvc.perform(post("/api/vicios/{vicioId}/regras", vicio.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.impactoEstresse").value(10))
                .andExpect(jsonPath("$.debuffNome").value("Ressaca"));
    }

    @Test
    void getAllRegras_whenExists_shouldReturnList() throws Exception {
        var vicio = vicioRepository.save(new Vicio(VicioId.generate(), "Alcoolismo", null));
        regraVicioRepository.save(new RegraVicio(RegraVicioId.generate(), vicio, 10, -50, 8, null, null));

        mockMvc.perform(get("/api/vicios/{vicioId}/regras", vicio.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deleteRegra_whenExists_shouldReturn204() throws Exception {
        var vicio = vicioRepository.save(new Vicio(VicioId.generate(), "Alcoolismo", null));
        var regra = regraVicioRepository.save(new RegraVicio(RegraVicioId.generate(), vicio, 10, -50, 8, null, null));

        mockMvc.perform(delete("/api/vicios/{vicioId}/regras/{regraId}", vicio.getId().getValue(), regra.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        assertThat(regraVicioRepository.findById(regra.getId())).isEmpty();
    }
}
