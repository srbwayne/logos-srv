package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.vicio.application.dto.RegraVicioDto;
import com.josecjuniors.logossrv.core.vicio.application.port.in.GetAllRegrasByVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import com.josecjuniors.logossrv.core.vicio.domain.repository.RegraVicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetAllRegrasByVicioService implements GetAllRegrasByVicioUseCase {

    private final RegraVicioRepository repository;

    public GetAllRegrasByVicioService(RegraVicioRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RegraVicioDto> getAll(VicioId vicioId) {
        return repository.findAllByVicioId(vicioId).stream()
                .map(RegraVicioDto::fromDomain)
                .collect(Collectors.toList());
    }
}
