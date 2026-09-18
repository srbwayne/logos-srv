package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.progression.application.port.in.ProvisionCurrentExternalSubjectIdentityUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionSubjectIdentityProvisioningPort;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisionCurrentExternalSubjectIdentityService
        implements ProvisionCurrentExternalSubjectIdentityUseCase {

    private static final String LOGOS_NATIVE_NAMESPACE = "logos-native";

    private final JogadorRepository jogadorRepository;
    private final ProgressionSubjectIdentityProvisioningPort provisioning;

    public ProvisionCurrentExternalSubjectIdentityService(
            JogadorRepository jogadorRepository,
            ProgressionSubjectIdentityProvisioningPort provisioning) {
        this.jogadorRepository = jogadorRepository;
        this.provisioning = provisioning;
    }

    @Override
    @Transactional
    public ExternalSubjectReference provision(String authenticatedUserEmail,
                                              ExternalSubjectReference reference) {
        if (LOGOS_NATIVE_NAMESPACE.equals(reference.namespace())) {
            throw new IllegalArgumentException("logos-native namespace is managed by Logos");
        }
        Jogador jogador = jogadorRepository.findByUserEmail(authenticatedUserEmail)
                .orElseThrow(JogadorNaoEncontradoException::new);
        provisioning.provision(reference, new SubjectId(jogador.getUser().getId().getValue()));
        return reference;
    }
}
