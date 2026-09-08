package com.josecjuniors.logossrv.core.vicio.application.command;

import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;

public record CreateRegraVicioCommand(
        VicioId vicioId,
        Integer impactoEstresse,
        Integer penalidadePontos,
        Integer duracaoHoras,
        Integer xpGanhoRecaida,
        DebuffId debuffId
) {}
