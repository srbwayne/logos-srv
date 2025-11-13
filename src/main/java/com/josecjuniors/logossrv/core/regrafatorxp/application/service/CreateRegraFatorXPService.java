package com.josecjuniors.logossrv.core.regrafatorxp.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoNaoEncontradoException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.exception.RegraDistribuicaoNaoEncontradaException;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import com.josecjuniors.logossrv.core.regrafatorxp.application.dto.RegraFatorXPDto;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.CreateRegraFatorXPCommand;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.CreateRegraFatorXPUseCase;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.repository.RegraFatorXPRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateRegraFatorXPService implements CreateRegraFatorXPUseCase {

    private final RegraFatorXPRepository regraFatorXPRepository;
    private final RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository;
    private final FatorCalculoRepository fatorCalculoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateRegraFatorXPService(RegraFatorXPRepository regraFatorXPRepository, RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository, FatorCalculoRepository fatorCalculoRepository, ApplicationEventPublisher eventPublisher) {
        this.regraFatorXPRepository = regraFatorXPRepository;
        this.regraDistribuicaoRepository = regraDistribuicaoRepository;
        this.fatorCalculoRepository = fatorCalculoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RegraFatorXPDto create(CreateRegraFatorXPCommand command) {
        RegraDistribuicaoAtividade regraDistribuicao = regraDistribuicaoRepository.findById(command.regraDistribuicaoId())
                .orElseThrow(RegraDistribuicaoNaoEncontradaException::new);
        FatorCalculo fatorCalculo = fatorCalculoRepository.findById(command.fatorCalculoId())
                .orElseThrow(FatorCalculoNaoEncontradoException::new);

        RegraFatorXP novaRegra = new RegraFatorXP(
                new RegraFatorXPId(),
                regraDistribuicao,
                fatorCalculo,
                command.pesoMultiplicador(),
                command.pontoCorteMin(),
                command.pontoCorteMax()
        );

        RegraFatorXP regraSalva = regraFatorXPRepository.save(novaRegra);

        eventPublisher.publishEvent(new AtividadeConfigSalvaEvent(regraDistribuicao.getAtividadeConfig().getId()));

        return RegraFatorXPDto.fromDomain(regraSalva);
    }
}
