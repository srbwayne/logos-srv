package com.josecjuniors.logossrv.core.registrovicio.application.command;

import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;

import java.time.LocalDateTime;

public record CreateRegistroVicioCommand(
        String userEmail,
        VicioId vicioId,
        LocalDateTime dataHora,
        String observacao
) {}
