package com.josecjuniors.logossrv.core.registrovicio.application.dto;

import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicio;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistroVicioDto(
        UUID id,
        UUID vicioJogadorId,
        LocalDateTime dataHora,
        String observacao
) {
    public static RegistroVicioDto fromDomain(RegistroVicio domain) {
        return new RegistroVicioDto(
                domain.getId().getValue(),
                domain.getVicioJogador().getId().getValue(),
                domain.getDataHora(),
                domain.getObservacao()
        );
    }
}
