package com.josecjuniors.logossrv.core.atividadeconfig.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.DeleteAtividadeConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteAtividadeConfigService implements DeleteAtividadeConfigUseCase {

    private final AtividadeConfigRepository repository;

    public DeleteAtividadeConfigService(AtividadeConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public void delete(AtividadeConfigId id) {
        repository.deleteById(id);
    }
}
