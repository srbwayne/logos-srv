package com.josecjuniors.logossrv.core.atividadeconfig.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.UpdateAtividadeConfigCommand;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.UpdateAtividadeConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigJaExisteException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateAtividadeConfigService implements UpdateAtividadeConfigUseCase {

    private final AtividadeConfigRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateAtividadeConfigService(AtividadeConfigRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AtividadeConfigDto update(UpdateAtividadeConfigCommand command) {
        AtividadeConfig atividade = repository.findById(command.atividadeConfigId())
                .orElseThrow(AtividadeConfigNaoEncontradaException::new);

        if (repository.existsByNomeAndIdNot(command.nome(), command.atividadeConfigId())) {
            throw new AtividadeConfigJaExisteException(command.nome());
        }

        atividade.atualizar(
                command.nome(),
                command.descricao(),
                command.xpBase(),
                command.estresseBase(),
                command.diasParaPenalidade(),
                command.xpPerdaPorCiclo()
        );

        AtividadeConfig atividadeAtualizada = repository.save(atividade);

        eventPublisher.publishEvent(new AtividadeConfigSalvaEvent(atividadeAtualizada.getId()));

        return AtividadeConfigDto.fromDomain(atividadeAtualizada);
    }
}
