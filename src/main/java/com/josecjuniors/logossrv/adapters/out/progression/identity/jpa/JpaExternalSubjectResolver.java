package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.progression.application.port.out.ExternalSubjectResolver;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import org.springframework.stereotype.Component;

@Component
public class JpaExternalSubjectResolver implements ExternalSubjectResolver {

    private final ProgressionSubjectIdentityJpaRepository repository;

    public JpaExternalSubjectResolver(ProgressionSubjectIdentityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubjectId resolve(ExternalSubjectReference reference) {
        return repository.findByNamespaceAndExternalId(reference.namespace(), reference.externalId())
                .map(identity -> new SubjectId(identity.getJogador().getUser().getId().getValue()))
                .orElseThrow(ProgressionSubjectNotFoundException::new);
    }
}
