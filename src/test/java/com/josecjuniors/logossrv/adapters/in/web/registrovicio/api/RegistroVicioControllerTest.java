package com.josecjuniors.logossrv.adapters.in.web.registrovicio.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.josecjuniors.logossrv.adapters.in.web.registrovicio.dto.request.CreateRegistroVicioRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.jogador.domain.repository.VicioJogadorRepository;
import com.josecjuniors.logossrv.core.registrovicio.domain.repository.RegistroVicioRepository;
import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import com.josecjuniors.logossrv.core.vicio.domain.repository.VicioRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class RegistroVicioControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegistroVicioRepository registroVicioRepository;
    @Autowired
    private VicioJogadorRepository vicioJogadorRepository;
    @Autowired
    private VicioRepository vicioRepository;
    @Autowired
    private JogadorRepository jogadorRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private Jogador jogador;
    private Vicio vicio;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        registroVicioRepository.deleteAll();
        vicioJogadorRepository.deleteAll();
        vicioRepository.deleteAll();
        jogadorRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser testAppUser = new AppUser(new AppUserId(), "regvicio.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);

        jogador = jogadorRepository.save(new Jogador(JogadorId.generate(), testAppUser, "Viciado"));
        vicio = vicioRepository.save(new Vicio(VicioId.generate(), "Cafeína", "Tomar muito café"));
    }

    @Test
    void create_whenVicioJogadorExists_shouldReturn202() throws Exception {
        vicioJogadorRepository.save(new VicioJogador(VicioJogadorId.generate(), jogador, vicio));
        var request = new CreateRegistroVicioRequest(vicio.getId().getValue(), LocalDateTime.now(), "Tomei um expresso duplo.");

        mockMvc.perform(post("/api/registros-vicio")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void create_whenVicioJogadorDoesNotExist_shouldCreateItAndReturn202() throws Exception {
        long countAntes = vicioJogadorRepository.countByJogador_Id(jogador.getId());
        var request = new CreateRegistroVicioRequest(vicio.getId().getValue(), LocalDateTime.now(), "Primeira vez registrando este vício.");

        mockMvc.perform(post("/api/registros-vicio")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        long countDepois = vicioJogadorRepository.countByJogador_Id(jogador.getId());
        assertThat(countDepois).isEqualTo(countAntes + 1);
        assertThat(vicioJogadorRepository.findByJogadorIdAndVicioId(jogador.getId(), vicio.getId())).isPresent();
    }

    @Test
    void create_whenVicioDoesNotExist_shouldReturn404() throws Exception {
        var request = new CreateRegistroVicioRequest(UUID.randomUUID(), LocalDateTime.now(), "Vício que não existe.");

        mockMvc.perform(post("/api/registros-vicio")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withoutToken_shouldReturn403() throws Exception {
        var request = new CreateRegistroVicioRequest(vicio.getId().getValue(), LocalDateTime.now(), "Sem token");

        mockMvc.perform(post("/api/registros-vicio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
