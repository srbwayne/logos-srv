package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.CreateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.UpdateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.ReplaceAtividadeFormularioRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormularioId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    private AtividadeFormularioRepository atividadeFormularioRepository;
    @Autowired
    private FatorCalculoRepository fatorCalculoRepository;
    @Autowired
    private RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository;
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
        atividadeFormularioRepository.deleteAll();
        atividadeConfigRepository.deleteAll();
        fatorCalculoRepository.deleteAll();
        appUserRepository.deleteAll();
        AppUser testAppUser = new AppUser(new AppUserId(), "atividade.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        jwtToken = jwtService.generateToken(testAppUser);
    }

    @Test
    void create_withValidData_shouldReturn201() throws Exception {
        CreateAtividadeConfigRequest request = new CreateAtividadeConfigRequest("Corrida", "Corrida ao ar livre", null, null, null, null);

        mockMvc.perform(post("/api/atividades-config")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Corrida"));
    }

    @Test
    void create_bootstrapsEmptyFormBeforeReturning() throws Exception {
        CreateAtividadeConfigRequest request = new CreateAtividadeConfigRequest("Leitura", "Ler", null, null, null, null);

        String location = mockMvc.perform(post("/api/atividades-config")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        mockMvc.perform(get("/api/atividades-config/{id}/formulario", id)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Activity-Form-Version", "1"))
                .andExpect(jsonPath("$.campos", hasSize(0)));
    }

    @Test
    void updateMetadataPreservesAuthoredCaptureVersionAndFields() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", "Inicial", 50, 1, null, null));
        FatorCalculo paginas = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Páginas", "pág", TipoInput.NUMERICO));
        AtividadeFormularioJson form = new AtividadeFormularioJson(atividade.getId().getValue(), atividade.getNome(), atividade.getDescricao(),
                java.util.List.of(new com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.CampoFormularioJson(
                        paginas.getId().getValue(), paginas.getNome(), paginas.getUnidadeMedida(), paginas.getTipoInput(), "Páginas lidas", true)));
        atividadeFormularioRepository.save(new AtividadeFormulario(AtividadeFormularioId.generate(), atividade, form));

        UpdateAtividadeConfigRequest request = new UpdateAtividadeConfigRequest("Leitura diária", "Atualizada", null, null, null, null);
        mockMvc.perform(put("/api/atividades-config/{id}", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/atividades-config/{id}/formulario", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Activity-Form-Version", "1"))
                .andExpect(jsonPath("$.nomeAtividade").value("Leitura diária"))
                .andExpect(jsonPath("$.descricaoAtividade").value("Atualizada"))
                .andExpect(jsonPath("$.campos", hasSize(1)))
                .andExpect(jsonPath("$.campos[0].placeholder").value("Páginas lidas"));
    }

    @Test
    void create_whenNameIsTaken_shouldReturn409() throws Exception {
        atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", null, 50, -5, null, null));
        CreateAtividadeConfigRequest request = new CreateAtividadeConfigRequest("Leitura", null, null, null, null, null);

        mockMvc.perform(post("/api/atividades-config")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_withLegacyProgressionFields_shouldReturn410() throws Exception {
        CreateAtividadeConfigRequest request = new CreateAtividadeConfigRequest("Corrida", "Cat?logo", 100, null, null, null);

        mockMvc.perform(post("/api/atividades-config")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }

    @Test
    void update_withLegacyProgressionFields_shouldReturn410() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida", "Inicial", null, null, null, null));
        UpdateAtividadeConfigRequest request = new UpdateAtividadeConfigRequest("Corrida atualizada", "Cat?logo", null, 1, null, null);

        mockMvc.perform(put("/api/atividades-config/{id}", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
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
        UpdateAtividadeConfigRequest request = new UpdateAtividadeConfigRequest("Treino de Força", "Foco em hipertrofia", null, null, null, null);

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
        UpdateAtividadeConfigRequest request = new UpdateAtividadeConfigRequest("Ciclismo", null, null, null, null, null);

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

    @Test
    void delete_withLegacyProgressionHistory_shouldReturn409AndPreserveRows() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Histórica", null, null, null, null, null));
        Atributo atributo = atributoRepository.save(new Atributo(new AtributoId(), "Foco", null));
        RegraDistribuicaoAtividade regra = regraDistribuicaoRepository.save(new RegraDistribuicaoAtividade(
                new RegraDistribuicaoAtividadeId(), atividade, atributo, 1.0));

        mockMvc.perform(delete("/api/atividades-config/{id}", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isConflict());

        org.assertj.core.api.Assertions.assertThat(atividadeConfigRepository.findById(atividade.getId())).isPresent();
        org.assertj.core.api.Assertions.assertThat(regraDistribuicaoRepository.findById(regra.getId())).isPresent();
    }

    @Test
    void getFormulario_whenFormularioExists_shouldReturn200AndFormularioJson() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", "Ler um livro", 50, -10, null, null));
        AtividadeFormularioJson json = new AtividadeFormularioJson(atividade.getId().getValue(), atividade.getNome(), atividade.getDescricao(), new ArrayList<>());
        atividadeFormularioRepository.save(new AtividadeFormulario(AtividadeFormularioId.generate(), atividade, json));

        mockMvc.perform(get("/api/atividades-config/{id}/formulario", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atividadeConfigId").value(atividade.getId().getValue().toString()))
                        .andExpect(jsonPath("$.nomeAtividade").value("Leitura"));
    }

    @Test
    void getFormulario_exposesCaptureVersionInHeaderWithoutChangingBody() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", "Ler", 50, -10, null, null));
        AtividadeFormularioJson json = new AtividadeFormularioJson(atividade.getId().getValue(), atividade.getNome(), atividade.getDescricao(), new ArrayList<>());
        atividadeFormularioRepository.save(new AtividadeFormulario(AtividadeFormularioId.generate(), atividade, json));

        mockMvc.perform(get("/api/atividades-config/{id}/formulario", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Activity-Form-Version", "1"))
                .andExpect(jsonPath("$.campos", hasSize(0)));
    }

    @Test
    void replaceFormulario_returnsIncrementedVersionHeader() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", "Ler", 50, -10, null, null));
        AtividadeFormularioJson json = new AtividadeFormularioJson(atividade.getId().getValue(), atividade.getNome(), atividade.getDescricao(), new ArrayList<>());
        atividadeFormularioRepository.save(new AtividadeFormulario(AtividadeFormularioId.generate(), atividade, json));
        FatorCalculo paginas = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Páginas", "pág", TipoInput.NUMERICO));
        ReplaceAtividadeFormularioRequest request = new ReplaceAtividadeFormularioRequest(1,
                java.util.List.of(new ReplaceAtividadeFormularioRequest.CampoRequest(paginas.getId().getValue(), "Páginas lidas")));

        mockMvc.perform(put("/api/atividades-config/{id}/formulario", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Activity-Form-Version", "2"))
                .andExpect(jsonPath("$.campos", hasSize(1)))
                .andExpect(jsonPath("$.campos[0].placeholder").value("Páginas lidas"));
    }

    @Test
    void replaceFormulario_withStaleVersionReturnsConflict() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", "Ler", 50, -10, null, null));
        AtividadeFormularioJson json = new AtividadeFormularioJson(atividade.getId().getValue(), atividade.getNome(), atividade.getDescricao(), new ArrayList<>());
        atividadeFormularioRepository.save(new AtividadeFormulario(AtividadeFormularioId.generate(), atividade, json));
        ReplaceAtividadeFormularioRequest request = new ReplaceAtividadeFormularioRequest(0, new ArrayList<>());

        mockMvc.perform(put("/api/atividades-config/{id}/formulario", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getFormulario_whenFormularioDoesNotExist_shouldReturn404() throws Exception {
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", "Ler um livro", 50, -10, null, null));

        mockMvc.perform(get("/api/atividades-config/{id}/formulario", atividade.getId().getValue())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }
}
