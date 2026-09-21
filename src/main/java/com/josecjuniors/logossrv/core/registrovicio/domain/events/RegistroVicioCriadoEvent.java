package com.josecjuniors.logossrv.core.registrovicio.domain.events;

import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicioId;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;

public record RegistroVicioCriadoEvent(
        RegistroVicioId registroVicioId,
        JogadorId jogadorId
) {}
