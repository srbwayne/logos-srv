package com.josecjuniors.logossrv.core.regrafatorxp.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.DeleteRegraFatorXPUseCase;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.exception.RegraFatorXPNaoEncontradaException;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.repository.RegraFatorXPRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteRegraFatorXPService implements DeleteRegraFatorXPUseCase {

    private final RegraFatorXPRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DeleteRegraFatorXPService(RegraFatorXPRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void delete(RegraFatorXPId id) {
        throw new LegacyProgressionAuthoringRetiredException();

    }
}
