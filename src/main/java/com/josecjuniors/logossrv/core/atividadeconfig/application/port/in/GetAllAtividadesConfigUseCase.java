package com.josecjuniors.logossrv.core.atividadeconfig.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAllAtividadesConfigUseCase {
    Page<AtividadeConfigDto> getAll(String searchTerm, Pageable pageable);
}
