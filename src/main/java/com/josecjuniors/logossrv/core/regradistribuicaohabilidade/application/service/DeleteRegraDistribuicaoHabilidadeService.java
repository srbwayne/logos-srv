package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.service;

import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.DeletarRegradistribuicaoHabilidadeCommand;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in.DeleteRegraDistribuicaoHabilidadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.exception.RegraDistribuicaoHabilidadeNaoEncontradaException;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.exception.RegradistribuicaoHabilidadeNaoPertenceAEstaHabilidadeException;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.repository.RegraDistribuicaoHabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteRegraDistribuicaoHabilidadeService implements DeleteRegraDistribuicaoHabilidadeUseCase {

    private final RegraDistribuicaoHabilidadeRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public DeleteRegraDistribuicaoHabilidadeService(RegraDistribuicaoHabilidadeRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public DeleteRegraDistribuicaoHabilidadeService(RegraDistribuicaoHabilidadeRepository repository) {
        this(repository, null);
    }

    @Override
    public void delete(DeletarRegradistribuicaoHabilidadeCommand command) {
        var regraDistribuicaoHabilidade = repository.findById(command.regraDistribuicaoHabilidadeId())
                .orElseThrow(RegraDistribuicaoHabilidadeNaoEncontradaException::new);

        if (!regraDistribuicaoHabilidade.getHabilidade().getId().equals(command.habilidadeId())) {
            throw new RegradistribuicaoHabilidadeNaoPertenceAEstaHabilidadeException();
        }
        repository.deleteById(regraDistribuicaoHabilidade.getId());
        if (eventPublisher != null) eventPublisher.publishEvent(new com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.events.SkillPolicySalvaEvent());
    }
}
