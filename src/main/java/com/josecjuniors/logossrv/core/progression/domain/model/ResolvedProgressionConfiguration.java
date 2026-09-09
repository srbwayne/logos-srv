package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.Set;
import java.util.UUID;

public record ResolvedProgressionConfiguration(
        UUID configurationVersionId,
        UUID skillPolicyVersionId,
        ProgressionConfiguration configuration,
        Set<String> numericFactorKeys,
        FactKeyGeneration factKeyGeneration
) {
    public ResolvedProgressionConfiguration(UUID configurationVersionId, UUID skillPolicyVersionId,
                                            ProgressionConfiguration configuration, Set<String> numericFactorKeys) {
        this(configurationVersionId, skillPolicyVersionId, configuration, numericFactorKeys, FactKeyGeneration.SEMANTIC);
    }

    public ResolvedProgressionConfiguration {
        numericFactorKeys = Set.copyOf(numericFactorKeys);
    }
}
