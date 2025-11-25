package com.josecjuniors.logossrv.adapters.in.web.vicio.dto.response;

import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;

import java.util.UUID;

public record VicioResponse(UUID id, String nome, String descricao) {
    public static VicioResponse fromDto(VicioDto dto) {
        return new VicioResponse(dto.id(), dto.nome(), dto.descricao());
    }
}
