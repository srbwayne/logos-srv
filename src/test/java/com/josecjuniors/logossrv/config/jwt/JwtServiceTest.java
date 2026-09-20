package com.josecjuniors.logossrv.config.jwt;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private static final long EXPIRATION = 86_400_000L;

    @Test
    void applicationConfigurationRequiresRuntimeJwtSecret() throws IOException {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(Path.of("src", "main", "resources", "application.properties"))) {
            properties.load(input);
        }

        assertEquals("${LOGOS_JWT_SECRET}", properties.getProperty("application.security.jwt.secret-key"));
    }

    @Test
    void configuredSigningMaterialGeneratesAndValidatesToken() {
        JwtService jwtService = jwtServiceWith(testKey("current"));
        var user = user("user@example.test");

        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenSignedWithPreviousKeyIsRejectedAfterRotation() {
        var user = user("user@example.test");
        String token = jwtServiceWith(testKey("previous")).generateToken(user);
        JwtService rotatedJwtService = jwtServiceWith(testKey("current"));

        assertThrows(JwtException.class, () -> rotatedJwtService.isTokenValid(token, user));
    }

    private JwtService jwtServiceWith(String signingMaterial) {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", signingMaterial);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
        return jwtService;
    }

    private String testKey(String keyVersion) {
        return Base64.getEncoder().encodeToString(
                ("test-only-jwt-signing-material-" + keyVersion + "-sufficiently-long")
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private org.springframework.security.core.userdetails.UserDetails user(String username) {
        var user = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(user.getUsername()).thenReturn(username);
        return user;
    }
}
