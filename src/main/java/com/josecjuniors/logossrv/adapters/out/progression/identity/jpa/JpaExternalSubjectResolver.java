package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.progression.application.port.out.ExternalSubjectResolver;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;

@Component
public class JpaExternalSubjectResolver implements ExternalSubjectResolver {

    private final ProgressionSubjectIdentityJpaRepository repository;

    public JpaExternalSubjectResolver(ProgressionSubjectIdentityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubjectId resolve(ExternalSubjectReference reference) {
        var current = repository.findCurrentAppUserId(reference.namespace(), reference.externalId());
        if (current.isEmpty()
                && repository.existsByNamespaceAndExternalId(reference.namespace(), reference.externalId())) {
            throw new DataIntegrityViolationException("Subject identity exists without a current binding pointer");
        }
        return current
                .map(AppUserId::new)
                .map(appUserId -> new SubjectId(appUserId.getValue()))
                .orElseThrow(ProgressionSubjectNotFoundException::new);
    }
}
