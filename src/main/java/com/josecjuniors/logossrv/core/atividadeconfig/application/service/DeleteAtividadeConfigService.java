package com.josecjuniors.logossrv.core.atividadeconfig.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.DeleteAtividadeConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigEmUsoException;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.out.ActivityDeletionDependencyQuery;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteAtividadeConfigService implements DeleteAtividadeConfigUseCase {

    private final AtividadeConfigRepository repository;
    private final AtividadeFormularioRepository formularioRepository;
    private final ActivityDeletionDependencyQuery dependencyQuery;

    public DeleteAtividadeConfigService(AtividadeConfigRepository repository, AtividadeFormularioRepository formularioRepository,
                                        ActivityDeletionDependencyQuery dependencyQuery) {
        this.repository = repository;
        this.formularioRepository = formularioRepository;
        this.dependencyQuery = dependencyQuery;
    }

    @Override
    public void delete(AtividadeConfigId id) {
        repository.findByIdForUpdate(id).orElseThrow(AtividadeConfigNaoEncontradaException::new);
        if (dependencyQuery.hasProgressionDefinition(id)
                || dependencyQuery.hasRegistroAtividade(id)
                || dependencyQuery.hasAtividadeAgendada(id)) {
            throw new AtividadeConfigEmUsoException();
        }
        formularioRepository.deleteByAtividadeConfigId(id);
        repository.deleteById(id);
    }
}
