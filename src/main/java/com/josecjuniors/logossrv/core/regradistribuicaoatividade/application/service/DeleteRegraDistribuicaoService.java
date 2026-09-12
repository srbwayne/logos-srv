package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.DeleteRegraDistribuicaoUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.exception.RegraDistribuicaoNaoEncontradaException;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteRegraDistribuicaoService implements DeleteRegraDistribuicaoUseCase {

    private final RegraDistribuicaoAtividadeRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DeleteRegraDistribuicaoService(RegraDistribuicaoAtividadeRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void delete(RegraDistribuicaoAtividadeId id) {
        throw new LegacyProgressionAuthoringRetiredException();

    }
}
