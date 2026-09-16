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
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormularioId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.CampoFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

    private String jwtToken;
    private Jogador testJogador;
    private AtividadeConfig testAtividadeConfig;
    private FatorCalculo fatorDistancia;
    @Autowired
    private AtividadeFormularioRepository atividadeFormularioRepository;

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
testAtividadeConfig = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida", null));
        fatorDistancia = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Distância", "km", TipoInput.NUMERICO, "distance_km"));
        atividadeFormularioRepository.save(new AtividadeFormulario(AtividadeFormularioId.generate(), testAtividadeConfig,
                new AtividadeFormularioJson(testAtividadeConfig.getId().getValue(), testAtividadeConfig.getNome(), testAtividadeConfig.getDescricao(),
                        List.of(new CampoFormularioJson(fatorDistancia.getId().getValue(), fatorDistancia.getNome(), fatorDistancia.getUnidadeMedida(), fatorDistancia.getTipoInput(), "", true)))));
        entityManager.flush();
        UUID definition = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, legacy_atividade_config_id, current_version_id) VALUES (?, ?, ?, NULL)",
                definition, "activity:" + testAtividadeConfig.getId().getValue(), testAtividadeConfig.getId().getValue());
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress, fact_key_generation) VALUES (?, ?, 1, 100, 10, 'SEMANTIC')",
                version, definition);
        jdbc.update("INSERT INTO progression_configuration_version_factor(id, configuration_version_id, factor_key, tipo_input) VALUES (?, ?, ?, 'NUMERICO')",
                UUID.randomUUID(), version, fatorDistancia.getSemanticKey());
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?", version, definition);
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
                1,
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
                1,
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
                1,
                List.of()
        );

        mockMvc.perform(post("/api/registros-atividade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutFormVersion_shouldReturn400WithoutPersistence() throws Exception {
        CreateRegistroAtividadeRequest request = request(null, List.of(
                new DetalheRegistroRequest(fatorDistancia.getId().getValue(), "10.5")));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_VERSION_REQUIRED"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withStaleFormVersion_shouldReturn409WithoutPersistence() throws Exception {
        CreateRegistroAtividadeRequest request = request(0, List.of(
                new DetalheRegistroRequest(fatorDistancia.getId().getValue(), "10.5")));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_VERSION_CONFLICT"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withDuplicateField_shouldReturn400WithoutPersistence() throws Exception {
        CreateRegistroAtividadeRequest request = request(1, List.of(
                new DetalheRegistroRequest(fatorDistancia.getId().getValue(), "10.5"),
                new DetalheRegistroRequest(fatorDistancia.getId().getValue(), "11.5")));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_DUPLICATE_FIELD"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withFieldOutsideForm_shouldReturn400WithoutPersistence() throws Exception {
        FatorCalculo outsideForm = fatorCalculoRepository.save(
                new FatorCalculo(FatorCalculoId.generate(), "Fora", "un", TipoInput.NUMERICO, "outside_form"));
        CreateRegistroAtividadeRequest request = request(1, List.of(
                new DetalheRegistroRequest(outsideForm.getId().getValue(), "10.5")));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_FIELD_NOT_ALLOWED"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withRequiredFieldMissing_shouldReturn400WithoutPersistence() throws Exception {
        CreateRegistroAtividadeRequest request = request(1, List.of());

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_REQUIRED_FIELD_MISSING"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withInvalidNumericValue_shouldReturn400WithoutPersistence() throws Exception {
        CreateRegistroAtividadeRequest request = request(1, List.of(
                new DetalheRegistroRequest(fatorDistancia.getId().getValue(), "1,5")));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_INVALID_VALUE"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_whenLiveFactDefinitionIsMissing_shouldReturn409WithoutPersistence() throws Exception {
        UUID factorId = fatorDistancia.getId().getValue();
        fatorCalculoRepository.deleteById(fatorDistancia.getId());

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(1,
                                List.of(new DetalheRegistroRequest(factorId, "10.5"))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_FACT_DEFINITION_MISMATCH"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_whenLiveFactDefinitionTypeDiverges_shouldReturn409WithoutPersistence() throws Exception {
        fatorDistancia.atualizar(fatorDistancia.getNome(), fatorDistancia.getUnidadeMedida(), TipoInput.TEXTO_CURTO);
        fatorCalculoRepository.save(fatorDistancia);

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(1,
                                List.of(new DetalheRegistroRequest(fatorDistancia.getId().getValue(), "10.5"))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_FACT_DEFINITION_MISMATCH"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withNumericExponent_shouldReturn202() throws Exception {
        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(1,
                                List.of(new DetalheRegistroRequest(fatorDistancia.getId().getValue(), "1.5e-2"))))))
                .andExpect(status().isAccepted());
    }

    @Test
    void create_withNumericWhitespace_shouldReturn400WithoutPersistence() throws Exception {
        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(1,
                                List.of(new DetalheRegistroRequest(fatorDistancia.getId().getValue(), " 1"))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_INVALID_VALUE"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withForbiddenNumericValues_shouldReturn400WithoutPersistence() throws Exception {
        for (String value : List.of("NaN", "Infinity", "-Infinity", "1e309", "0x10", ".5", "1.")) {
            mockMvc.perform(post("/api/registros-atividade")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request(1,
                                    List.of(new DetalheRegistroRequest(fatorDistancia.getId().getValue(), value))))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_INVALID_VALUE"));
        }

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withTextField_persistsButDoesNotCreateNumericFact() throws Exception {
        FatorCalculo text = fatorCalculoRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Comentário", null, TipoInput.TEXTO_LONGO, "comment_text"));
        replaceForm(List.of(new CampoFormularioJson(text.getId().getValue(), text.getNome(), text.getUnidadeMedida(),
                text.getTipoInput(), "", true)));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(2,
                                List.of(new DetalheRegistroRequest(text.getId().getValue(), "x".repeat(100)))))))
                .andExpect(status().isAccepted());

        assertThat(registroAtividadeRepository.findAll()).hasSize(1)
                .first().extracting(r -> r.getDetalhes().get(0).getValorRegistrado())
                .isEqualTo("x".repeat(100));
    }

    @Test
    void create_withTextLengthOverPersistenceBoundary_shouldReturn400() throws Exception {
        FatorCalculo text = fatorCalculoRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Comentário", null, TipoInput.TEXTO_LONGO, "comment_text"));
        replaceForm(List.of(new CampoFormularioJson(text.getId().getValue(), text.getNome(), text.getUnidadeMedida(),
                text.getTipoInput(), "", true)));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(2,
                                List.of(new DetalheRegistroRequest(text.getId().getValue(), "x".repeat(101)))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_INVALID_VALUE"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withBlankRequiredText_shouldReturn400() throws Exception {
        FatorCalculo text = fatorCalculoRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Comentário", null, TipoInput.TEXTO_CURTO, "comment_text"));
        replaceForm(List.of(new CampoFormularioJson(text.getId().getValue(), text.getNome(), text.getUnidadeMedida(),
                text.getTipoInput(), "", true)));

        for (String value : new String[]{null, "", " "}) {
            mockMvc.perform(post("/api/registros-atividade")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request(2,
                                    List.of(new DetalheRegistroRequest(text.getId().getValue(), value))))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_INVALID_VALUE"));
        }

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withSingleSelectionUuid_isAcceptedAndPersisted() throws Exception {
        FatorCalculo selection = fatorCalculoRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Opção", null, TipoInput.SELECAO_UNICA, "single_option"));
        replaceForm(List.of(new CampoFormularioJson(selection.getId().getValue(), selection.getNome(), selection.getUnidadeMedida(),
                selection.getTipoInput(), "", true)));
        String optionId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(2,
                                List.of(new DetalheRegistroRequest(selection.getId().getValue(), optionId))))))
                .andExpect(status().isAccepted());
    }

    @Test
    void create_withMalformedSingleSelectionUuid_shouldReturn400() throws Exception {
        FatorCalculo selection = fatorCalculoRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Opção", null, TipoInput.SELECAO_UNICA, "single_option"));
        replaceForm(List.of(new CampoFormularioJson(selection.getId().getValue(), selection.getNome(), selection.getUnidadeMedida(),
                selection.getTipoInput(), "", true)));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(2,
                                List.of(new DetalheRegistroRequest(selection.getId().getValue(), "not-a-uuid"))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_INVALID_VALUE"));
    }

    @Test
    void create_withNumericFormFieldAbsentFromActiveConfiguration_shouldReturn409() throws Exception {
        FatorCalculo outsideConfiguration = fatorCalculoRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Outra métrica", "un", TipoInput.NUMERICO, "outside_configuration"));
        replaceForm(List.of(
                new CampoFormularioJson(fatorDistancia.getId().getValue(), fatorDistancia.getNome(), fatorDistancia.getUnidadeMedida(), fatorDistancia.getTipoInput(), "", true),
                new CampoFormularioJson(outsideConfiguration.getId().getValue(), outsideConfiguration.getNome(), outsideConfiguration.getUnidadeMedida(), outsideConfiguration.getTipoInput(), "", true)));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(2, List.of(
                                new DetalheRegistroRequest(fatorDistancia.getId().getValue(), "10.5"),
                                new DetalheRegistroRequest(outsideConfiguration.getId().getValue(), "2"))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_PROGRESSION_CONFIGURATION_MISMATCH"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    @Test
    void create_withMultipleSelection_isExplicitlyUnsupported() throws Exception {
        FatorCalculo selection = fatorCalculoRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Opções", null, TipoInput.SELECAO_MULTIPLA, "multiple_options"));
        replaceForm(List.of(new CampoFormularioJson(selection.getId().getValue(), selection.getNome(), selection.getUnidadeMedida(),
                selection.getTipoInput(), "", true)));

        mockMvc.perform(post("/api/registros-atividade")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(2,
                                List.of(new DetalheRegistroRequest(selection.getId().getValue(), "anything"))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_FORM_INPUT_TYPE_UNSUPPORTED"));

        assertThat(registroAtividadeRepository.findAll()).isEmpty();
    }

    private void replaceForm(List<CampoFormularioJson> campos) {
        AtividadeFormulario formulario = atividadeFormularioRepository
                .findByAtividadeConfigId(testAtividadeConfig.getId()).orElseThrow();
        formulario.replaceCaptureDefinition(new AtividadeFormularioJson(
                testAtividadeConfig.getId().getValue(), testAtividadeConfig.getNome(), testAtividadeConfig.getDescricao(), campos));
        atividadeFormularioRepository.save(formulario);
    }

    private CreateRegistroAtividadeRequest request(Integer formVersion, List<DetalheRegistroRequest> detalhes) {
        return new CreateRegistroAtividadeRequest(
                testAtividadeConfig.getId().getValue(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                formVersion,
                detalhes);
    }
}
