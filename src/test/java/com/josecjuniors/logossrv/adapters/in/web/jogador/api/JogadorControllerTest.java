package com.josecjuniors.logossrv.adapters.in.web.jogador.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.request.UpdateApelidoRequest;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.request.UpdatePerfilRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.AtributoJogadorJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogadorId;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private AtributoRepository atributoRepository;
    @Autowired
    private AtributoJogadorJpaRepository atributoJogadorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private Jogador testJogador;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        appUserRepository.deleteAll();
        AppUser testAppUser = new AppUser(new AppUserId(), "perfil.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        testJogador = new Jogador(new JogadorId(), testAppUser, "Testador");
        jogadorRepository.save(testJogador);
        jwtToken = jwtService.generateToken(testAppUser);
    }

    @Test
    void getMeuResumo_withValidToken_shouldReturn200AndResumo() throws Exception {
        mockMvc.perform(get("/api/jogadores/meu-resumo")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apelido").value("Testador"))
                .andExpect(jsonPath("$.nivelAtual").value(1))
                .andExpect(jsonPath("$.xpTotal").value(0))
                .andExpect(jsonPath("$.xpParaProximoNivel").value(150)); // (1*1 * 100) + 50
    }

    @Test
    void getMeuResumo_withoutToken_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/jogadores/meu-resumo"))
                .andExpect(status().isForbidden());
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

    @Test
    void getMeusAtributos_shouldReturnPagedAndSortedAtributos() throws Exception {
        Atributo forca = atributoRepository.save(new Atributo(new AtributoId(), "Força", ""));
        Atributo agilidade = atributoRepository.save(new Atributo(new AtributoId(), "Agilidade", ""));
        Atributo inteligencia = atributoRepository.save(new Atributo(new AtributoId(), "Inteligência", ""));
        atributoJogadorRepository.save(new AtributoJogador(new AtributoJogadorId(), testJogador, forca));
        atributoJogadorRepository.save(new AtributoJogador(new AtributoJogadorId(), testJogador, agilidade));
        atributoJogadorRepository.save(new AtributoJogador(new AtributoJogadorId(), testJogador, inteligencia));

        mockMvc.perform(get("/api/jogadores/meu-perfil/atributos")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "atributo.nome,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].nome").value("Agilidade"))
                .andExpect(jsonPath("$.content[0].xpParaProximoNivel").value(150))
                .andExpect(jsonPath("$.content[1].nome").value("Força"));
    }

    @Test
    void getMeusAtributos_whenPlayerHasNoAtributos_shouldReturnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/jogadores/meu-perfil/atributos")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getMeusAtributos_withoutToken_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/jogadores/meu-perfil/atributos"))
                .andExpect(status().isForbidden());
    }
}
