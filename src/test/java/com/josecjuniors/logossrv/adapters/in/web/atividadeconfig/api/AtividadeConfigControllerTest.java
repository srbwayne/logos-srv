package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.CreateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.UpdateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class AtividadeConfigControllerTest {
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private AtividadeConfigRepository repository;
  @Autowired
  private AtividadeFormularioRepository formularioRepository;
  @Autowired
  private FatorCalculoRepository fatorCalculoRepository;
  @Autowired
  private EntityManager entityManager;
  @Autowired
  private AppUserJpaRepository users;
  @Autowired
  private PasswordEncoder passwordEncoder;
  @Autowired
  private JwtService jwtService;
  private String token;

  @BeforeEach
  void setUp() {
    formularioRepository.deleteAll();
    repository.deleteAll();
    users.deleteAll();
    var user =
        new AppUser(
            new AppUserId(), "atividade.test@email.com", passwordEncoder.encode("password"));
    users.save(user);
    token = jwtService.generateToken(user);
  }

  @Test
  void create_withValidData_shouldReturn201AndBootstrapForm() throws Exception {
    var request =
        new CreateAtividadeConfigRequest("Corrida", "Ao ar livre", null, null, null, null);
    var result =
        mockMvc
            .perform(
                post("/api/atividades-config")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nome").value("Corrida"))
            .andReturn();
    var id = UUID.fromString(result.getResponse().getHeader("Location").replaceAll(".*/", ""));
    mockMvc
        .perform(
            get("/api/atividades-config/{id}/formulario", id)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Activity-Form-Version", "1"))
        .andExpect(jsonPath("$.campos").isArray());
  }

  @Test
  void create_withLegacyProgressionFields_shouldReturn410WithoutActivity() throws Exception {
    var request = new CreateAtividadeConfigRequest("Corrida", "Catálogo", 100, null, null, null);
    mockMvc
        .perform(
            post("/api/atividades-config")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isGone());
    org.assertj.core.api.Assertions.assertThat(repository.existsByNome("Corrida")).isFalse();
  }

  @Test
  void getById_andGetAll_returnCanonicalMetadataWithoutLegacyFields() throws Exception {
    var activity =
        repository.save(new AtividadeConfig(new AtividadeConfigId(), "Estudar", "Descrição"));
    mockMvc
        .perform(
            get("/api/atividades-config/{id}", activity.getId().getValue())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.nome").value("Estudar"))
        .andExpect(jsonPath("$.xpBase").doesNotExist())
        .andExpect(jsonPath("$.estresseBase").doesNotExist())
        .andExpect(jsonPath("$.diasParaPenalidade").doesNotExist())
        .andExpect(jsonPath("$.xpPerdaPorCiclo").doesNotExist());
    mockMvc
        .perform(get("/api/atividades-config").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].descricao").value("Descrição"))
        .andExpect(jsonPath("$.content[0].xpBase").doesNotExist());
  }

  @Test
  void update_withLegacyProgressionFields_shouldReturn410() throws Exception {
    var activity =
        repository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida", "Inicial"));
    var request = new UpdateAtividadeConfigRequest("Atualizada", "Descrição", null, 1, null, null);
    mockMvc
        .perform(
            put("/api/atividades-config/{id}", activity.getId().getValue())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isGone());
  }

  @Test
  void getMissing_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get("/api/atividades-config/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void listSearch_shouldReturnOnlyMatchingCanonicalActivityWithoutLegacyFields() throws Exception {
    repository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida", "Ao ar livre"));
    repository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", "Em casa"));
    mockMvc
        .perform(
            get("/api/atividades-config")
                .header("Authorization", "Bearer " + token)
                .param("searchTerm", "Corrida"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$.content[0].nome").value("Corrida"))
        .andExpect(jsonPath("$.content[0].xpBase").doesNotExist())
        .andExpect(jsonPath("$.content[0].estresseBase").doesNotExist())
        .andExpect(jsonPath("$.content[0].diasParaPenalidade").doesNotExist())
        .andExpect(jsonPath("$.content[0].xpPerdaPorCiclo").doesNotExist());
  }

  @Test
  void updateMetadata_shouldReturn200AndChangeCanonicalFields() throws Exception {
    var activity =
        repository.save(new AtividadeConfig(new AtividadeConfigId(), "Inicial", "Antes"));
    var request = new UpdateAtividadeConfigRequest("Atualizada", "Depois", null, null, null, null);
    mockMvc
        .perform(
            put("/api/atividades-config/{id}", activity.getId().getValue())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Atualizada"))
        .andExpect(jsonPath("$.descricao").value("Depois"));
  }

  @Test
  void duplicateNames_onCreateAndUpdate_shouldReturn409() throws Exception {
    repository.save(new AtividadeConfig(new AtividadeConfigId(), "Duplicada", "Primeira"));
    var create = new CreateAtividadeConfigRequest("Duplicada", "Outra", null, null, null, null);
    mockMvc
        .perform(
            post("/api/atividades-config")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)))
        .andExpect(status().isConflict());
    var second = repository.save(new AtividadeConfig(new AtividadeConfigId(), "Segunda", "Outra"));
    var update =
        new UpdateAtividadeConfigRequest("Duplicada", "Atualização", null, null, null, null);
    mockMvc
        .perform(
            put("/api/atividades-config/{id}", second.getId().getValue())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isConflict());
  }

  @Test
  void eachLegacyCreateField_shouldReturn410WithoutActivityOrForm() throws Exception {
    String[] fields = {"xpBase", "estresseBase", "diasParaPenalidade", "xpPerdaPorCiclo"};
    for (int i = 0; i < fields.length; i++) {
      String name = "RetiredCreate" + i;
      String body = "{\"nome\":\"" + name + "\",\"descricao\":\"desc\",\"" + fields[i] + "\":1}";
      mockMvc
          .perform(
              post("/api/atividades-config")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isGone());
      org.assertj.core.api.Assertions.assertThat(repository.existsByNome(name)).isFalse();
    }
  }

  @Test
  void eachLegacyUpdateField_shouldReturn410WithoutChangingMetadata() throws Exception {
    var activity =
        repository.save(new AtividadeConfig(new AtividadeConfigId(), "Stable", "Before"));
    String[] fields = {"xpBase", "estresseBase", "diasParaPenalidade", "xpPerdaPorCiclo"};
    for (String field : fields) {
      String body = "{\"nome\":\"Changed\",\"descricao\":\"Changed\",\"" + field + "\":1}";
      mockMvc
          .perform(
              put("/api/atividades-config/{id}", activity.getId().getValue())
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isGone());
    }
    var persisted = repository.findById(activity.getId()).orElseThrow();
    org.assertj.core.api.Assertions.assertThat(persisted.getNome()).isEqualTo("Stable");
    org.assertj.core.api.Assertions.assertThat(persisted.getDescricao()).isEqualTo("Before");
  }

  @Test
  void canonicalCreatedUnusedActivity_shouldDeleteActivityAndOwnedForm() throws Exception {
    var request = new CreateAtividadeConfigRequest("ToDelete", "Unused", null, null, null, null);
    var result =
        mockMvc
            .perform(
                post("/api/atividades-config")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();
    UUID id = UUID.fromString(result.getResponse().getHeader("Location").replaceAll(".*/", ""));
    mockMvc
        .perform(
            delete("/api/atividades-config/{id}", id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    org.assertj.core.api.Assertions.assertThat(repository.findById(new AtividadeConfigId(id)))
        .isEmpty();
    org.assertj.core.api.Assertions.assertThat(
            formularioRepository.findByAtividadeConfigId(new AtividadeConfigId(id)))
        .isEmpty();
  }

  @Test
  void formReplacement_shouldIncrementVersionAndRejectStaleVersion() throws Exception {
    var request = new CreateAtividadeConfigRequest("Form", "Owned", null, null, null, null);
    var result =
        mockMvc
            .perform(
                post("/api/atividades-config")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();
    UUID id = UUID.fromString(result.getResponse().getHeader("Location").replaceAll(".*/", ""));
    String replacement = "{\"expectedVersion\":1,\"campos\":[]}";
    mockMvc
        .perform(
            put("/api/atividades-config/{id}/formulario", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(replacement))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Activity-Form-Version", "2"));
    mockMvc
        .perform(
            put("/api/atividades-config/{id}/formulario", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(replacement))
        .andExpect(status().isConflict());
  }

  @Test
  void metadataUpdatePreservesAuthoredFormState() throws Exception {
    var activity =
        repository.save(new AtividadeConfig(new AtividadeConfigId(), "Leitura", "Antes"));
    var factor =
        fatorCalculoRepository.save(
            new FatorCalculo(
                FatorCalculoId.generate(),
                "Páginas " + UUID.randomUUID(),
                "páginas",
                TipoInput.NUMERICO));
    entityManager.flush();

    String authoredForm =
        "{\"expectedVersion\":1,\"campos\":[{\"fatorCalculoId\":\""
            + factor.getId().getValue()
            + "\",\"placeholder\":\"Páginas lidas\"}]}";
    mockMvc
        .perform(
            put("/api/atividades-config/{id}/formulario", activity.getId().getValue())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(authoredForm))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Activity-Form-Version", "2"));

    var metadata =
        new UpdateAtividadeConfigRequest("Leitura diária", "Depois", null, null, null, null);
    mockMvc
        .perform(
            put("/api/atividades-config/{id}", activity.getId().getValue())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(metadata)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/atividades-config/{id}/formulario", activity.getId().getValue())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Activity-Form-Version", "2"))
        .andExpect(jsonPath("$.nomeAtividade").value("Leitura diária"))
        .andExpect(jsonPath("$.descricaoAtividade").value("Depois"))
        .andExpect(jsonPath("$.campos", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(
            jsonPath("$.campos[0].fatorCalculoId").value(factor.getId().getValue().toString()))
        .andExpect(jsonPath("$.campos[0].placeholder").value("Páginas lidas"))
        .andExpect(jsonPath("$.campos[0].obrigatorio").value(true));
  }
}
