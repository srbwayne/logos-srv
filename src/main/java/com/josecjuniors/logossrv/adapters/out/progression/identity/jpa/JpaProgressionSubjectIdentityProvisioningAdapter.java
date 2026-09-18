package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionSubjectIdentityProvisioningPort;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectIdentityConflictException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class JpaProgressionSubjectIdentityProvisioningAdapter
        implements ProgressionSubjectIdentityProvisioningPort {

    private final ProgressionSubjectIdentityJpaRepository repository;
    private final JogadorRepository jogadorRepository;

    public JpaProgressionSubjectIdentityProvisioningAdapter(
            ProgressionSubjectIdentityJpaRepository repository,
            JogadorRepository jogadorRepository) {
        this.repository = repository;
        this.jogadorRepository = jogadorRepository;
    }

    @Override
    @Transactional
    public void provision(ExternalSubjectReference reference, SubjectId target) {
        var jogador = jogadorRepository.findByAppUserId(new AppUserId(target.value()))
                .orElseThrow(JogadorNaoEncontradoException::new);
        repository.insertIfAbsent(UUID.randomUUID(), reference.namespace(), reference.externalId(),
                jogador.getId().getValue());

        repository.findAppUserIdByNamespaceAndExternalId(reference.namespace(), reference.externalId())
                .filter(owner -> owner.getValue().equals(target.value()))
                .orElseThrow(ProgressionSubjectIdentityConflictException::new);
    }
}
