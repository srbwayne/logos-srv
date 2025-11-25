package com.josecjuniors.logossrv.core.vicio.application.port.in;

import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAllViciosUseCase {
    Page<VicioDto> getAll(String searchTerm, Pageable pageable);
}
