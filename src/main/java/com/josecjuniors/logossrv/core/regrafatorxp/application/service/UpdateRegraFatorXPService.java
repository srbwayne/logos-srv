package com.josecjuniors.logossrv.core.regrafatorxp.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoNaoEncontradoException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.regrafatorxp.application.dto.RegraFatorXPDto;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.UpdateRegraFatorXPCommand;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.UpdateRegraFatorXPUseCase;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.exception.RegraFatorXPNaoEncontradaException;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.repository.RegraFatorXPRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateRegraFatorXPService implements UpdateRegraFatorXPUseCase {

    private final RegraFatorXPRepository regraFatorXPRepository;
    private final FatorCalculoRepository fatorCalculoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateRegraFatorXPService(RegraFatorXPRepository regraFatorXPRepository, FatorCalculoRepository fatorCalculoRepository, ApplicationEventPublisher eventPublisher) {
        this.regraFatorXPRepository = regraFatorXPRepository;
        this.fatorCalculoRepository = fatorCalculoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RegraFatorXPDto update(UpdateRegraFatorXPCommand command) {
        RegraFatorXP regra = regraFatorXPRepository.findById(command.regraFatorXPId())
                .orElseThrow(RegraFatorXPNaoEncontradaException::new);
        FatorCalculo fatorCalculo = fatorCalculoRepository.findById(command.fatorCalculoId())
                .orElseThrow(FatorCalculoNaoEncontradoException::new);

        regra.atualizar(
                fatorCalculo,
                command.pesoMultiplicador(),
                command.pontoCorteMin(),
                command.pontoCorteMax()
        );

        RegraFatorXP regraAtualizada = regraFatorXPRepository.save(regra);

        eventPublisher.publishEvent(new AtividadeConfigSalvaEvent(regra.getRegraDistribuicaoAtividade().getAtividadeConfig().getId()));

        return RegraFatorXPDto.fromDomain(regraAtualizada);
    }
}
