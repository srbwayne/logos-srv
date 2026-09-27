package com.josecjuniors.logossrv.config.security.workload;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

/** Unregistered response adapter shared by future workload filter wiring and tests. */
public final class WorkloadAuthenticationFailureResponder {
    public void respond(HttpServletResponse response, AuthenticationException failure) throws IOException {
        response.resetBuffer();
        response.setStatus(failure instanceof AuthenticationServiceException
                ? HttpServletResponse.SC_SERVICE_UNAVAILABLE
                : HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentLength(0);
    }
}
