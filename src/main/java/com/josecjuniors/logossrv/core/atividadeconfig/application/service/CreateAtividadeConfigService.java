package com.josecjuniors.logossrv.core.atividadeconfig.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.CreateAtividadeConfigCommand;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.CreateAtividadeConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigJaExisteException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateAtividadeConfigService implements CreateAtividadeConfigUseCase {

    private final AtividadeConfigRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateAtividadeConfigService(AtividadeConfigRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AtividadeConfigDto create(CreateAtividadeConfigCommand command) {
        if (repository.existsByNome(command.nome())) {
            throw new AtividadeConfigJaExisteException(command.nome());
        }
        AtividadeConfig novaAtividade = new AtividadeConfig(
                new AtividadeConfigId(),
                command.nome(),
                command.descricao(),
                command.xpBase(),
                command.estresseBase(),
                command.diasParaPenalidade(),
                command.xpPerdaPorCiclo()
        );
        AtividadeConfig atividadeSalva = repository.save(novaAtividade);

        eventPublisher.publishEvent(new AtividadeConfigSalvaEvent(atividadeSalva.getId()));

        return AtividadeConfigDto.fromDomain(atividadeSalva);
    }
}
