package com.josecjuniors.logossrv.core.registroatividade.application.port.in;

import com.josecjuniors.logossrv.core.registroatividade.domain.events.RegistroAtividadeCriadoEvent;

public interface ProcessarRegistroAtividadeUseCase {
    void processar(RegistroAtividadeCriadoEvent event);
}
