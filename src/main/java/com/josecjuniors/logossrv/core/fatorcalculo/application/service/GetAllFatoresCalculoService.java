package com.josecjuniors.logossrv.core.fatorcalculo.application.service;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.GetAllFatoresCalculoUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAllFatoresCalculoService implements GetAllFatoresCalculoUseCase {

    private final FatorCalculoRepository repository;

    public GetAllFatoresCalculoService(FatorCalculoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<FatorCalculoDto> getAll(String searchTerm, Pageable pageable) {
        return repository.findByNomeContainingIgnoreCase(searchTerm, pageable)
                .map(FatorCalculoDto::fromDomain);
    }
}
