package com.josecjuniors.logossrv.adapters.in.web.registrovicio.dto.response;

import com.josecjuniors.logossrv.core.registrovicio.application.dto.RegistroVicioDto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistroVicioResponse(
        UUID id,
        LocalDateTime dataHora
) {
    public static RegistroVicioResponse fromDto(RegistroVicioDto dto) {
        return new RegistroVicioResponse(dto.id(), dto.dataHora());
    }
}
