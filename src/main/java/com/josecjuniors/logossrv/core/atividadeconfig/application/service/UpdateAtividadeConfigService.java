package com.josecjuniors.logossrv.core.atividadeconfig.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.UpdateAtividadeConfigCommand;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.UpdateAtividadeConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeCatalogoSalvoEvent;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
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
        rejectLegacyProgressionFields(command.xpBase(), command.estresseBase(), command.diasParaPenalidade(), command.xpPerdaPorCiclo());
        AtividadeConfig atividade = repository.findById(command.atividadeConfigId())
                .orElseThrow(AtividadeConfigNaoEncontradaException::new);

        if (repository.existsByNomeAndIdNot(command.nome(), command.atividadeConfigId())) {
            throw new AtividadeConfigJaExisteException(command.nome());
        }

        atividade.atualizarCatalogo(command.nome(), command.descricao());

        AtividadeConfig atividadeAtualizada = repository.save(atividade);

        eventPublisher.publishEvent(new AtividadeCatalogoSalvoEvent(atividadeAtualizada.getId()));

        return AtividadeConfigDto.fromDomain(atividadeAtualizada);
    }

    private void rejectLegacyProgressionFields(Integer xpBase, Integer estresseBase, Integer diasParaPenalidade, Integer xpPerdaPorCiclo) {
        if (xpBase != null || estresseBase != null || diasParaPenalidade != null || xpPerdaPorCiclo != null) {
            throw new LegacyProgressionAuthoringRetiredException();
        }
    }
}
