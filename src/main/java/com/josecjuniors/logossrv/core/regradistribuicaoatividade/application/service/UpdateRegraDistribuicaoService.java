package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.UpdateRegraDistribuicaoCommand;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.UpdateRegraDistribuicaoUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.exception.RegraDistribuicaoNaoEncontradaException;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateRegraDistribuicaoService implements UpdateRegraDistribuicaoUseCase {

    private final RegraDistribuicaoAtividadeRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateRegraDistribuicaoService(RegraDistribuicaoAtividadeRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RegraDistribuicaoAtividadeDto update(UpdateRegraDistribuicaoCommand command) {
        throw new LegacyProgressionAuthoringRetiredException();

    }
}
