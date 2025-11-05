package com.josecjuniors.logossrv.core.atributo.application.port.in;

import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAllAtributosUseCase {
    Page<AtributoDto> getAll(String searchTerm, Pageable pageable);
}
