package com.josecjuniors.logossrv.core.atividadeconfig.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.GetAtividadeConfigByIdUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetAtividadeConfigByIdService implements GetAtividadeConfigByIdUseCase {

    private final AtividadeConfigRepository repository;

    public GetAtividadeConfigByIdService(AtividadeConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AtividadeConfigDto> getById(AtividadeConfigId id) {
        return repository.findById(id).map(AtividadeConfigDto::fromDomain);
    }
}
