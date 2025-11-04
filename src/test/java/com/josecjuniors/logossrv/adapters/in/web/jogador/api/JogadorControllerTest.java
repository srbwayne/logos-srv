package com.josecjuniors.logossrv.adapters.in.web.jogador.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.request.UpdateApelidoRequest;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.request.UpdatePerfilRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.EnderecoEstado;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.Sexo;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.StatusPerfilJogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class JogadorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserJpaRepository appUserRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private AppUser testAppUser;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        appUserRepository.deleteAll();

        testAppUser = new AppUser(new AppUserId(), "perfil.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);

        Jogador testJogador = new Jogador(new JogadorId(), testAppUser, "Testador");
        jogadorRepository.save(testJogador);

        jwtToken = jwtService.generateToken(testAppUser);
    }

    @Test
    void updateMeuPerfil_withValidDataAndToken_shouldReturn200AndUpdatedProfile() throws Exception {
        UpdatePerfilRequest request = new UpdatePerfilRequest("Nome Completo", LocalDate.now(), "123", "999", Sexo.MASCULINO, "Desc", "BR", EnderecoEstado.SP, "SP", "Rua", "Comp", "123");

        mockMvc.perform(put("/api/jogadores/meu-perfil")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPerfil").value(StatusPerfilJogador.COMPLETO.toString()));
    }

    @Test
    void updateMeuPerfil_withoutToken_shouldReturn403Forbidden() throws Exception {
        UpdatePerfilRequest request = new UpdatePerfilRequest(null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/jogadores/meu-perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMeuApelido_withValidDataAndToken_shouldReturn200AndUpdatedProfile() throws Exception {
        UpdateApelidoRequest request = new UpdateApelidoRequest("NovoApelido");

        mockMvc.perform(patch("/api/jogadores/meu-perfil/apelido")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apelido").value("NovoApelido"));
    }

    @Test
    void updateMeuApelido_whenApelidoIsTaken_shouldReturn409Conflict() throws Exception {
        AppUser anotherUser = new AppUser(new AppUserId(), "another@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(anotherUser);
        jogadorRepository.save(new Jogador(new JogadorId(), anotherUser, "ApelidoOcupado"));

        UpdateApelidoRequest request = new UpdateApelidoRequest("ApelidoOcupado");

        mockMvc.perform(patch("/api/jogadores/meu-perfil/apelido")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateMeuApelido_withoutToken_shouldReturn403Forbidden() throws Exception {
        UpdateApelidoRequest request = new UpdateApelidoRequest("QualquerApelido");

        mockMvc.perform(patch("/api/jogadores/meu-perfil/apelido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
