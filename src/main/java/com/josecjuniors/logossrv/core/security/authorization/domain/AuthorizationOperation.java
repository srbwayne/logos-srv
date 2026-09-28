package com.josecjuniors.logossrv.core.security.authorization.domain;

public enum AuthorizationOperation {
    PROGRESSION_EXECUTE(true, true),
    PROGRESSION_EXECUTION_READ(true, false),
    PROGRESSION_HISTORY_READ(false, true),
    SUBJECT_PROVISION(false, true);

    private final boolean requiresSource;
    private final boolean requiresNamespace;

    AuthorizationOperation(boolean requiresSource, boolean requiresNamespace) {
        this.requiresSource = requiresSource;
        this.requiresNamespace = requiresNamespace;
    }

    public boolean requiresSource() {
        return requiresSource;
    }

    public boolean requiresNamespace() {
        return requiresNamespace;
    }
}
