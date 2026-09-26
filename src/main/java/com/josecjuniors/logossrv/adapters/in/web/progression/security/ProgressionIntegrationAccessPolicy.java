package com.josecjuniors.logossrv.adapters.in.web.progression.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ProgressionIntegrationAccessPolicy {
    public void authorizeSource(ProgressionIntegrationPrincipal principal, String source) {
        if (principal == null || !principal.allowedSources().contains(normalize(source))) deny();
    }
    public void authorizeNamespace(ProgressionIntegrationPrincipal principal, String namespace) {
        if (principal == null || !principal.allowedNamespaces().contains(normalize(namespace))) deny();
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private void deny() { throw new AccessDeniedException("Progression integration client is not authorized for this resource"); }
}
