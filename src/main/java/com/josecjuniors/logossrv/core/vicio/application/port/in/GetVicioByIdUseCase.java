package com.josecjuniors.logossrv.core.vicio.application.port.in;

import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;

import java.util.Optional;

public interface GetVicioByIdUseCase {
    Optional<VicioDto> findById(VicioId id);
}
