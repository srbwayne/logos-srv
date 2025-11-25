package com.josecjuniors.logossrv.adapters.in.web.vicio.dto.response;

import com.josecjuniors.logossrv.core.vicio.application.dto.RegraVicioDto;

import java.util.UUID;

public record RegraVicioResponse(
        UUID id,
        Integer impactoEstresse,
        Integer penalidadePontos,
        Integer duracaoHoras,
        String debuffNome
) {
    public static RegraVicioResponse fromDto(RegraVicioDto dto) {
        return new RegraVicioResponse(
                dto.id(),
                dto.impactoEstresse(),
                dto.penalidadePontos(),
                dto.duracaoHoras(),
                dto.debuffNome()
        );
    }
}
