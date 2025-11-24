package com.josecjuniors.logossrv.core.vicio.application.dto;

import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicio;

import java.util.UUID;

public record RegraVicioDto(
        UUID id,
        UUID vicioId,
        Integer impactoEstresse,
        Integer penalidadePontos,
        Integer duracaoHoras,
        UUID debuffId,
        String debuffNome
) {
    public static RegraVicioDto fromDomain(RegraVicio domain) {
        return new RegraVicioDto(
                domain.getId().getValue(),
                domain.getVicio().getId().getValue(),
                domain.getImpactoEstresse(),
                domain.getPenalidadePontos(),
                domain.getDuracaoHoras(),
                domain.getDebuff() != null ? domain.getDebuff().getId().getValue() : null,
                domain.getDebuff() != null ? domain.getDebuff().getNome() : null
        );
    }
}
