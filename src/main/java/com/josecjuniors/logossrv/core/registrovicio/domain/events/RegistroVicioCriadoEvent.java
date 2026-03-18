package com.josecjuniors.logossrv.core.registrovicio.domain.events;

import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicioId;

public record RegistroVicioCriadoEvent(
        RegistroVicioId registroVicioId
) {}
