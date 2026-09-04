package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.Set;
import java.util.UUID;

public record ResolvedProgressionConfiguration(
        UUID configurationVersionId,
        UUID skillPolicyVersionId,
        ProgressionConfiguration configuration,
        Set<String> numericFactorKeys
) {
    public ResolvedProgressionConfiguration {
        numericFactorKeys = Set.copyOf(numericFactorKeys);
    }
}
