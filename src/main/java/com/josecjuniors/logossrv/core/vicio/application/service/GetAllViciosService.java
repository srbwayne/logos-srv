package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;
import com.josecjuniors.logossrv.core.vicio.application.port.in.GetAllViciosUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.repository.VicioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAllViciosService implements GetAllViciosUseCase {

    private final VicioRepository repository;

    public GetAllViciosService(VicioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<VicioDto> getAll(String searchTerm, Pageable pageable) {
        return repository.findByNomeContainingIgnoreCase(searchTerm, pageable).map(VicioDto::fromDomain);
    }
}
