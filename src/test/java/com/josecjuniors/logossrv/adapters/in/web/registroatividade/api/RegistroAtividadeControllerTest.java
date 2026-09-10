package com.josecjuniors.logossrv.adapters.in.web.registroatividade.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.registroatividade.dto.request.CreateRegistroAtividadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.registroatividade.dto.request.DetalheRegistroRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.enums.SituacaoRegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.enums.StatusProcessamento;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class RegistroAtividadeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegistroAtividadeRepository registroAtividadeRepository;
    @Autowired
    private AtividadeConfigRepository atividadeConfigRepository;
    @Autowired
    private FatorCalculoRepository fatorCalculoRepository;
    @Autowired
    private JogadorRepository jogadorRepository;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private Jogador testJogador;
    private AtividadeConfig testAtividadeConfig;
    private FatorCalculo fatorDistancia;

    @BeforeEach
    void setUp() {
        registroAtividadeRepository.deleteAll();
        atividadeConfigRepository.deleteAll();
        fatorCalculoRepository.deleteAll();
        jogadorRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser testAppUser = new AppUser(new AppUserId(), "registro.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);

        testJogador = jogadorRepository.save(new Jogador(JogadorId.generate(), testAppUser, "Registrador"));
        testAtividadeConfig = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida", null, 100, 10, null, null));
        fatorDistancia = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Distância", "km", TipoInput.NUMERICO, "distance_km"));
    }

    @Test
    void create_withValidData_shouldReturn202AndCreateRegistro() throws Exception {
        List<DetalheRegistroRequest> detalhes = List.of(
                new DetalheRegistroRequest(fatorDistancia.getId().getValue(), "10.5")
        );
        CreateRegistroAtividadeRequest request = new CreateRegistroAtividadeRequest(
                testAtividadeConfig.getId().getValue(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                detalhes
        );

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // Verificação no banco de dados
        List<RegistroAtividade> registros = registroAtividadeRepository.findAll();
        assertThat(registros).hasSize(1);
        RegistroAtividade registro = registros.get(0);
        assertThat(registro.getJogador().getId()).isEqualTo(testJogador.getId());
        assertThat(registro.getSituacao()).isEqualTo(SituacaoRegistroAtividade.CONCLUIDA);
        assertThat(registro.getStatusProcessamento()).isEqualTo(StatusProcessamento.PENDENTE);
        assertThat(registro.getDetalhes()).hasSize(1);
        assertThat(registro.getDetalhes().get(0).getFatorCalculo().getId()).isEqualTo(fatorDistancia.getId());
        assertThat(registro.getDetalhes().get(0).getValorRegistrado()).isEqualTo("10.5");
    }

    @Test
    void create_whenAtividadeConfigNotFound_shouldReturn404() throws Exception {
        CreateRegistroAtividadeRequest request = new CreateRegistroAtividadeRequest(
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                List.of()
        );

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withoutToken_shouldReturn403() throws Exception {
        CreateRegistroAtividadeRequest request = new CreateRegistroAtividadeRequest(
                testAtividadeConfig.getId().getValue(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                List.of()
        );

        mockMvc.perform(post("/api/registros-atividade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
