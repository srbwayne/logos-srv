package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;
import com.josecjuniors.logossrv.core.vicio.application.port.in.GetVicioByIdUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import com.josecjuniors.logossrv.core.vicio.domain.repository.VicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetVicioByIdService implements GetVicioByIdUseCase {

    private final VicioRepository repository;

    public GetVicioByIdService(VicioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<VicioDto> findById(VicioId id) {
        return repository.findById(id).map(VicioDto::fromDomain);
    }
}
