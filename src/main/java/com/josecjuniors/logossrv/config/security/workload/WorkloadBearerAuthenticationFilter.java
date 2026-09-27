package com.josecjuniors.logossrv.config.security.workload;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/** Dormant-by-default filter; it is deliberately not component-scanned or registered in F1E. */
public final class WorkloadBearerAuthenticationFilter extends OncePerRequestFilter {
    private static final int MAX_AUTHORIZATION_VALUE_LENGTH = 8192;

    private final AuthenticationManager authenticationManager;
    private final RequestMatcher workloadRequestMatcher;
    private final WorkloadAuthenticationFailureResponder failureResponder;

    public WorkloadBearerAuthenticationFilter(AuthenticationManager authenticationManager,
                                              RequestMatcher workloadRequestMatcher,
                                              WorkloadAuthenticationFailureResponder failureResponder) {
        this.authenticationManager = authenticationManager;
        this.workloadRequestMatcher = workloadRequestMatcher;
        this.failureResponder = failureResponder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !workloadRequestMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Never let a human or previously installed identity satisfy a workload-only match.
        SecurityContextHolder.clearContext();
        final String assertion;
        try {
            assertion = extractBearer(request);
        } catch (AuthenticationException failure) {
            failureResponder.respond(response, failure);
            return;
        }

        final Authentication result;
        try {
            result = authenticationManager.authenticate(WorkloadAssertionAuthenticationToken.unauthenticated(assertion));
            if (!(result instanceof WorkloadPrincipalAuthenticationToken workloadToken)
                    || !workloadToken.isAuthenticated()
                    || !(workloadToken.getPrincipal() instanceof AuthenticatedPrincipal)) {
                throw new AuthenticationServiceException("Authentication service unavailable");
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(workloadToken);
            SecurityContextHolder.setContext(context);
        } catch (AuthenticationException failure) {
            SecurityContextHolder.clearContext();
            failureResponder.respond(response, failure);
            return;
        } catch (RuntimeException failure) {
            SecurityContextHolder.clearContext();
            failureResponder.respond(response, new AuthenticationServiceException("Authentication service unavailable"));
            return;
        }
        chain.doFilter(request, response);
    }

    private static String extractBearer(HttpServletRequest request) {
        var values = Collections.list(request.getHeaders("Authorization"));
        if (values.size() != 1) throw unauthorized();
        String value = values.get(0);
        if (value == null || value.length() > MAX_AUTHORIZATION_VALUE_LENGTH) throw unauthorized();
        int separator = value.indexOf(' ');
        if (separator <= 0 || !"Bearer".equalsIgnoreCase(value.substring(0, separator))) throw unauthorized();
        String assertion = value.substring(separator + 1);
        if (assertion.isEmpty() || assertion.charAt(0) == ' ' || assertion.charAt(assertion.length() - 1) == ' '
                || assertion.indexOf(',') >= 0 || assertion.chars().anyMatch(Character::isWhitespace)) {
            throw unauthorized();
        }
        return assertion;
    }

    private static BadCredentialsException unauthorized() {
        return new BadCredentialsException("Unauthorized");
    }
}
