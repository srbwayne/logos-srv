package com.josecjuniors.logossrv.core.debuff.application.service;

import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;
import com.josecjuniors.logossrv.core.debuff.application.port.in.GetDebuffByIdUseCase;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetDebuffByIdService implements GetDebuffByIdUseCase {

    private final DebuffRepository repository;

    public GetDebuffByIdService(DebuffRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<DebuffDto> findById(DebuffId id) {
        return repository.findById(id).map(DebuffDto::fromDomain);
    }
}
