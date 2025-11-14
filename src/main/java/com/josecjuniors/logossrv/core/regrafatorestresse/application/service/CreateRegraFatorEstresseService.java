package com.josecjuniors.logossrv.core.regrafatorestresse.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.exception.RegraDistribuicaoNaoEncontradaException;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.dto.RegraFatorEstresseDto;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.CreateRegraFatorEstresseCommand;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.CreateRegraFatorEstresseUseCase;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.repository.RegraFatorEstresseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateRegraFatorEstresseService implements CreateRegraFatorEstresseUseCase {

    private final RegraFatorEstresseRepository regraFatorEstresseRepository;
    private final RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateRegraFatorEstresseService(RegraFatorEstresseRepository regraFatorEstresseRepository, RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository, ApplicationEventPublisher eventPublisher) {
        this.regraFatorEstresseRepository = regraFatorEstresseRepository;
        this.regraDistribuicaoRepository = regraDistribuicaoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RegraFatorEstresseDto create(CreateRegraFatorEstresseCommand command) {
        RegraDistribuicaoAtividade regraDistribuicao = regraDistribuicaoRepository.findById(command.regraDistribuicaoId())
                .orElseThrow(RegraDistribuicaoNaoEncontradaException::new);

        RegraFatorEstresse novaRegra = new RegraFatorEstresse(
                new RegraFatorEstresseId(),
                regraDistribuicao,
                command.pesoMultiplicador(),
                command.pontoCorteMin(),
                command.pontoCorteMax(),
                command.tipo()
        );

        RegraFatorEstresse regraSalva = regraFatorEstresseRepository.save(novaRegra);

        eventPublisher.publishEvent(new AtividadeConfigSalvaEvent(regraDistribuicao.getAtividadeConfig().getId()));

        return RegraFatorEstresseDto.fromDomain(regraSalva);
    }
}
