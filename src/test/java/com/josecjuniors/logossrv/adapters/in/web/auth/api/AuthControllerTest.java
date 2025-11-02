package com.josecjuniors.logossrv.adapters.in.web.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.auth.dto.request.LoginRequest;
import com.josecjuniors.logossrv.adapters.in.web.auth.dto.request.RegistrationRequest;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserJpaRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- REGISTER Tests ---

    @Test
    void register_withValidData_shouldReturn200AndCreateUserAndJogador() throws Exception {
        RegistrationRequest request = new RegistrationRequest("newuser@email.com", "password123", "Newbie");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.jogadorId").isNotEmpty())
                .andExpect(jsonPath("$.email").value("newuser@email.com"))
                .andExpect(jsonPath("$.nomeExibicao").value("Newbie"));
    }

    @Test
    void register_whenEmailAlreadyExists_shouldReturn409Conflict() throws Exception {
        // Arrange: Cria um usuário inicial
        appUserRepository.save(new AppUser(new AppUserId(), "existing@email.com", passwordEncoder.encode("password")));
        RegistrationRequest request = new RegistrationRequest("existing@email.com", "password123", "Newbie");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // --- LOGIN Tests ---

    @Test
    void login_withValidCredentials_shouldReturn200AndJwtToken() throws Exception {
        // Arrange
        appUserRepository.save(new AppUser(new AppUserId(), "user@email.com", passwordEncoder.encode("correct-password")));
        LoginRequest request = new LoginRequest("user@email.com", "correct-password");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void login_withInvalidPassword_shouldReturn403Forbidden() throws Exception {
        // Arrange
        appUserRepository.save(new AppUser(new AppUserId(), "user@email.com", passwordEncoder.encode("correct-password")));
        LoginRequest request = new LoginRequest("user@email.com", "wrong-password");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_whenUserDoesNotExist_shouldReturn403Forbidden() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent@email.com", "any-password");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
