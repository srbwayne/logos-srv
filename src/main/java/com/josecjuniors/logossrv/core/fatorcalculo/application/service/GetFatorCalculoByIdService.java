package com.josecjuniors.logossrv.core.fatorcalculo.application.service;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.GetFatorCalculoByIdUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetFatorCalculoByIdService implements GetFatorCalculoByIdUseCase {

    private final FatorCalculoRepository repository;

    public GetFatorCalculoByIdService(FatorCalculoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<FatorCalculoDto> getById(FatorCalculoId id) {
        return repository.findById(id).map(FatorCalculoDto::fromDomain);
    }
}
