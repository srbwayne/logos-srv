package com.josecjuniors.logossrv.core.regrafatorestresse.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.dto.RegraFatorEstresseDto;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.UpdateRegraFatorEstresseCommand;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.UpdateRegraFatorEstresseUseCase;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.exception.RegraFatorEstresseNaoEncontradaException;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.repository.RegraFatorEstresseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateRegraFatorEstresseService implements UpdateRegraFatorEstresseUseCase {

    private final RegraFatorEstresseRepository regraFatorEstresseRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateRegraFatorEstresseService(RegraFatorEstresseRepository regraFatorEstresseRepository, ApplicationEventPublisher eventPublisher) {
        this.regraFatorEstresseRepository = regraFatorEstresseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RegraFatorEstresseDto update(UpdateRegraFatorEstresseCommand command) {
        throw new LegacyProgressionAuthoringRetiredException();

    }
}
