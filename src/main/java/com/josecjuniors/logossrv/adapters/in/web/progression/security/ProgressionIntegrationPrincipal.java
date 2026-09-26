package com.josecjuniors.logossrv.adapters.in.web.progression.security;

import java.security.Principal;
import java.util.Set;

public record ProgressionIntegrationPrincipal(String clientId, Set<String> allowedSources,
                                              Set<String> allowedNamespaces) implements Principal {
    public static final String AUTHORITY = "PROGRESSION_INTEGRATION";
    @Override public String getName() { return clientId; }
}
