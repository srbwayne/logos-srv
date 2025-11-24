package com.josecjuniors.logossrv.adapters.in.web.debuff.dto.response;

import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;

import java.util.UUID;

public record DebuffResponse(UUID id, String nome) {
    public static DebuffResponse fromDto(DebuffDto dto) {
        return new DebuffResponse(dto.id(), dto.nome());
    }
}
