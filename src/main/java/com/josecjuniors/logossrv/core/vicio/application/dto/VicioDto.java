package com.josecjuniors.logossrv.core.vicio.application.dto;

import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;

import java.util.UUID;

public record VicioDto(
        UUID id,
        String nome,
        String descricao
) {
    public static VicioDto fromDomain(Vicio domain) {
        return new VicioDto(domain.getId().getValue(), domain.getNome(), domain.getDescricao());
    }
}
