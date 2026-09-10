package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.application.port.out.VersionedProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaProgressionConfigurationResolver implements ProgressionConfigurationResolver, VersionedProgressionConfigurationResolver, com.josecjuniors.logossrv.core.progression.application.port.out.ExternalProgressionConfigurationResolver {
    private final VersionedProgressionConfigurationStore store;

    public JpaProgressionConfigurationResolver(VersionedProgressionConfigurationStore store) {
        this.store = store;
    }

    @Override
    public Optional<ProgressionConfiguration> resolve(ProgressionConfigurationReference reference) {
        return resolveVersioned(reference).map(ResolvedProgressionConfiguration::configuration);
    }

    @Override
    public Optional<ResolvedProgressionConfiguration> resolveVersioned(ProgressionConfigurationReference reference) {
        return store.resolveVersioned(reference);
    }

    @Override
    public Optional<ResolvedProgressionConfiguration> resolveLegacyVersioned(ProgressionConfigurationReference reference) {
        return store.resolveLegacyVersioned(reference);
    }

    @Override
    public Optional<ResolvedProgressionConfiguration> resolve(ExternalProgressionConfigurationReference reference) {
        return store.resolveExternal(reference);
    }
}
