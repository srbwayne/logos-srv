package com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.dto.request.CreateAtividadeAgendadaRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.dto.request.ReagendarAtividadeRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendada;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendadaId;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.enums.StatusAtividadeAgendada;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.repository.AtividadeAgendadaRepository;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AtividadeAgendadaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AtividadeAgendadaRepository agendamentoRepository;
    @Autowired
    private AtividadeConfigRepository atividadeConfigRepository;
    @Autowired
    private JogadorRepository jogadorRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private EntityManager entityManager;

    private String jwtToken;
    private Jogador testJogador;
    private AtividadeConfig testAtividadeConfig;

    @BeforeEach
    void setUp() {
        agendamentoRepository.deleteAll();
        atividadeConfigRepository.deleteAll();
        jogadorRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser testAppUser = new AppUser(new AppUserId(), "agendamento.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);

        testJogador = jogadorRepository.save(new Jogador(JogadorId.generate(), testAppUser, "Agendador"));
        testAtividadeConfig = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Trabalho", null, 0, 0, null, null));
    }

    @Test
    void create_withValidData_shouldReturn201() throws Exception {
        CreateAtividadeAgendadaRequest request = new CreateAtividadeAgendadaRequest(
                testAtividadeConfig.getId().getValue(),
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2)
        );

        mockMvc.perform(post("/api/jogadores/{jogadorId}/atividades-agendadas", testJogador.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atividadeNome").value("Trabalho"))
                .andExpect(jsonPath("$.status").value("PLANEJADA"));
    }

    @Test
    void getByPeriodo_shouldReturn200AndListOfAgendamentos() throws Exception {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fim = inicio.plusDays(1);

        agendamentoRepository.save(new AtividadeAgendada(AtividadeAgendadaId.generate(), testJogador, testAtividadeConfig, inicio.plusHours(2), inicio.plusHours(3)));
        agendamentoRepository.save(new AtividadeAgendada(AtividadeAgendadaId.generate(), testJogador, testAtividadeConfig, inicio.plusHours(5), inicio.plusHours(6)));

        mockMvc.perform(get("/api/jogadores/{jogadorId}/atividades-agendadas", testJogador.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("inicio", inicio.toString())
                        .param("fim", fim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void reagendar_withValidData_shouldReturn200() throws Exception {
        AtividadeAgendada agendamento = agendamentoRepository.save(new AtividadeAgendada(AtividadeAgendadaId.generate(), testJogador, testAtividadeConfig, LocalDateTime.now(), LocalDateTime.now().plusHours(1)));
        ReagendarAtividadeRequest request = new ReagendarAtividadeRequest(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1)
        );

        mockMvc.perform(put("/api/jogadores/{jogadorId}/atividades-agendadas/{agendamentoId}", testJogador.getId().getValue(), agendamento.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REAGENDADA"));
    }

    @Test
    void reagendar_whenNotPlanejada_shouldReturn400() throws Exception {
        AtividadeAgendada agendamento = new AtividadeAgendada(AtividadeAgendadaId.generate(), testJogador, testAtividadeConfig, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        // Forçar o status para CONCLUIDA
        entityManager.persist(agendamento);
        agendamento.getClass().getDeclaredField("status").setAccessible(true);
        agendamento.getClass().getDeclaredField("status").set(agendamento, StatusAtividadeAgendada.CONCLUIDA);
        entityManager.flush();

        ReagendarAtividadeRequest request = new ReagendarAtividadeRequest(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));

        mockMvc.perform(put("/api/jogadores/{jogadorId}/atividades-agendadas/{agendamentoId}", testJogador.getId().getValue(), agendamento.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reagendar_whenNotOwner_shouldReturn403() throws Exception {
        AppUser outroAppUser = appUserRepository.save(new AppUser(new AppUserId(), "outro.user@email.com", "password"));
        Jogador outroJogador = jogadorRepository.save(new Jogador(JogadorId.generate(), outroAppUser, "Outro"));
        AtividadeAgendada agendamento = agendamentoRepository.save(new AtividadeAgendada(AtividadeAgendadaId.generate(), testJogador, testAtividadeConfig, LocalDateTime.now(), LocalDateTime.now().plusHours(1)));
        ReagendarAtividadeRequest request = new ReagendarAtividadeRequest(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));

        mockMvc.perform(put("/api/jogadores/{jogadorId}/atividades-agendadas/{agendamentoId}", outroJogador.getId().getValue(), agendamento.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
