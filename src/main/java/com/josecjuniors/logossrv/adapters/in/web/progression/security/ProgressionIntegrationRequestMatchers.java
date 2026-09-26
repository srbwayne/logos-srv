package com.josecjuniors.logossrv.adapters.in.web.progression.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public final class ProgressionIntegrationRequestMatchers {
    private static final RequestMatcher PROTECTED = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/internal/v1/progression/executions/**"),
            new AntPathRequestMatcher("/api/internal/v1/progression/subject-identities"));

    private ProgressionIntegrationRequestMatchers() { }
    public static RequestMatcher protectedEndpoints() { return PROTECTED; }
    public static boolean isProtected(HttpServletRequest request) { return PROTECTED.matches(request); }
}
