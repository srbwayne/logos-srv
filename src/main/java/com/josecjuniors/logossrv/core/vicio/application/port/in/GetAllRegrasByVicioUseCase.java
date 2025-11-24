package com.josecjuniors.logossrv.core.vicio.application.port.in;

import com.josecjuniors.logossrv.core.vicio.application.dto.RegraVicioDto;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;

import java.util.List;

public interface GetAllRegrasByVicioUseCase {
    List<RegraVicioDto> getAll(VicioId vicioId);
}
