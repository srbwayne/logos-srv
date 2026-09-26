package com.josecjuniors.logossrv.adapters.in.web.progression.security;

import com.josecjuniors.logossrv.config.progression.ProgressionIntegrationClientProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProgressionIntegrationAuthenticationFilter extends OncePerRequestFilter {
    public static final String CLIENT_ID_HEADER = "X-Logos-Client-Id";
    public static final String CLIENT_SECRET_HEADER = "X-Logos-Client-Secret";
    private final ProgressionIntegrationClientProperties properties;

    public ProgressionIntegrationAuthenticationFilter(ProgressionIntegrationClientProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ProgressionIntegrationRequestMatchers.isProtected(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String clientId = request.getHeader(CLIENT_ID_HEADER);
        String secret = request.getHeader(CLIENT_SECRET_HEADER);
        boolean supplied = clientId != null || secret != null;
        if (!supplied) {
            chain.doFilter(request, response);
            return;
        }
        if (request.getHeader("Authorization") != null || clientId == null || secret == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        var client = properties.getClients().stream()
                .filter(candidate -> clientId.equals(candidate.getClientId()))
                .filter(candidate -> matches(secret, candidate.getSecret()))
                .findFirst();
        if (client.isEmpty()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        var configured = client.get();
        var principal = new ProgressionIntegrationPrincipal(configured.getClientId(),
                normalized(configured.getAllowedSources()), normalized(configured.getAllowedNamespaces()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null,
                Set.of(new SimpleGrantedAuthority(ProgressionIntegrationPrincipal.AUTHORITY)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private boolean matches(String supplied, String configured) {
        if (configured == null) return false;
        return MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8), configured.getBytes(StandardCharsets.UTF_8));
    }

    private Set<String> normalized(java.util.List<String> values) {
        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }
}
