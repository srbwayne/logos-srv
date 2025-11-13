package com.josecjuniors.logossrv.core.atividadeconfig.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.GetAllAtividadesConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAllAtividadesConfigService implements GetAllAtividadesConfigUseCase {

    private final AtividadeConfigRepository repository;

    public GetAllAtividadesConfigService(AtividadeConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<AtividadeConfigDto> getAll(String searchTerm, Pageable pageable) {
        return repository.findByNomeContainingIgnoreCase(searchTerm, pageable)
                .map(AtividadeConfigDto::fromDomain);
    }
}
