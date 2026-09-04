package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.service;

import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.UpdateRegraDistribuicaoHabilidadeCommand;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.dto.RegraDistribuicaoHabilidadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in.UpdateRegraDistribuicaoHabilidadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.exception.RegraDistribuicaoHabilidadeNaoEncontradaException;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.exception.RegradistribuicaoHabilidadeNaoPertenceAEstaHabilidadeException;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidade;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.repository.RegraDistribuicaoHabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateRegraDistribuicaoHabilidadeService implements UpdateRegraDistribuicaoHabilidadeUseCase {

    private final RegraDistribuicaoHabilidadeRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public UpdateRegraDistribuicaoHabilidadeService(RegraDistribuicaoHabilidadeRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public UpdateRegraDistribuicaoHabilidadeService(RegraDistribuicaoHabilidadeRepository repository) {
        this(repository, null);
    }

    @Override
    public RegraDistribuicaoHabilidadeDto update(UpdateRegraDistribuicaoHabilidadeCommand command) {
        RegraDistribuicaoHabilidade regraDistribuicaoHabilidade = repository.findById(command.regraId())
                .orElseThrow(RegraDistribuicaoHabilidadeNaoEncontradaException::new);

        if (!regraDistribuicaoHabilidade.getHabilidade().getId().equals(command.habilidadeId())) {
            throw new RegradistribuicaoHabilidadeNaoPertenceAEstaHabilidadeException();
        }

        regraDistribuicaoHabilidade.atualizar(command.pesoDistribuicao());
        
        RegraDistribuicaoHabilidade regraSalva = repository.save(regraDistribuicaoHabilidade);
        if (eventPublisher != null) eventPublisher.publishEvent(new com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.events.SkillPolicySalvaEvent());
        return RegraDistribuicaoHabilidadeDto.fromDomain(regraSalva);
    }
}
