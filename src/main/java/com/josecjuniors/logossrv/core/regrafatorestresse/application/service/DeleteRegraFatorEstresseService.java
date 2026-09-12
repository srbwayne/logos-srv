package com.josecjuniors.logossrv.core.regrafatorestresse.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.DeleteRegraFatorEstresseUseCase;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.exception.RegraFatorEstresseNaoEncontradaException;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.repository.RegraFatorEstresseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteRegraFatorEstresseService implements DeleteRegraFatorEstresseUseCase {

    private final RegraFatorEstresseRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DeleteRegraFatorEstresseService(RegraFatorEstresseRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void delete(RegraFatorEstresseId id) {
        throw new LegacyProgressionAuthoringRetiredException();

    }
}
