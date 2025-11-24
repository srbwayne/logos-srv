package com.josecjuniors.logossrv.core.debuff.application.service;

import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;
import com.josecjuniors.logossrv.core.debuff.application.port.in.GetAllDebuffsUseCase;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAllDebuffsService implements GetAllDebuffsUseCase {

    private final DebuffRepository repository;

    public GetAllDebuffsService(DebuffRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<DebuffDto> getAll(String searchTerm, Pageable pageable) {
        return repository.findByNomeContainingIgnoreCase(searchTerm, pageable).map(DebuffDto::fromDomain);
    }
}
