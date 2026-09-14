package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.api;
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
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@IntegrationTest
class AtividadeConfigControllerTest {
 @Autowired MockMvc mockMvc; @Autowired ObjectMapper objectMapper; @Autowired AtividadeConfigRepository repository;
 @Autowired AtividadeFormularioRepository formularioRepository; @Autowired AppUserJpaRepository users;
 @Autowired PasswordEncoder passwordEncoder; @Autowired JwtService jwtService;
 private String token;
 @BeforeEach void setUp() { formularioRepository.deleteAll(); repository.deleteAll(); users.deleteAll(); var user=new AppUser(new AppUserId(),"atividade.test@email.com",passwordEncoder.encode("password")); users.save(user); token=jwtService.generateToken(user); }
 @Test void create_withValidData_shouldReturn201AndBootstrapForm() throws Exception {
  var request=new CreateAtividadeConfigRequest("Corrida","Ao ar livre",null,null,null,null);
  var result=mockMvc.perform(post("/api/atividades-config").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Corrida")).andReturn();
  var id=UUID.fromString(result.getResponse().getHeader("Location").replaceAll(".*/",""));
  mockMvc.perform(get("/api/atividades-config/{id}/formulario",id).header("Authorization","Bearer "+token)).andExpect(status().isOk());
 }
 @Test void create_withLegacyProgressionFields_shouldReturn410WithoutActivity() throws Exception {
  var request=new CreateAtividadeConfigRequest("Corrida","Catálogo",100,null,null,null);
  mockMvc.perform(post("/api/atividades-config").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isGone());
  org.assertj.core.api.Assertions.assertThat(repository.existsByNome("Corrida")).isFalse();
 }
 @Test void getById_andGetAll_returnCanonicalMetadataWithoutLegacyFields() throws Exception {
  var activity=repository.save(new AtividadeConfig(new AtividadeConfigId(),"Estudar","Descrição"));
  mockMvc.perform(get("/api/atividades-config/{id}",activity.getId().getValue()).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.nome").value("Estudar")).andExpect(jsonPath("$.xpBase").doesNotExist()).andExpect(jsonPath("$.estresseBase").doesNotExist()).andExpect(jsonPath("$.diasParaPenalidade").doesNotExist()).andExpect(jsonPath("$.xpPerdaPorCiclo").doesNotExist());
  mockMvc.perform(get("/api/atividades-config").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].descricao").value("Descrição")).andExpect(jsonPath("$.content[0].xpBase").doesNotExist());
 }
 @Test void update_withLegacyProgressionFields_shouldReturn410() throws Exception {
  var activity=repository.save(new AtividadeConfig(new AtividadeConfigId(),"Corrida","Inicial"));
  var request=new UpdateAtividadeConfigRequest("Atualizada","Descrição",null,1,null,null);
  mockMvc.perform(put("/api/atividades-config/{id}",activity.getId().getValue()).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isGone());
 }
}
