package com.josecjuniors.logossrv.core.vicio.application.command;

import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicioId;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;

public record DeletarRegravicioCommand(VicioId vicioId, RegraVicioId regraId) {
}
