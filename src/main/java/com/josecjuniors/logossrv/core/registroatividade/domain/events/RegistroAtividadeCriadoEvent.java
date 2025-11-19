package com.josecjuniors.logossrv.core.registroatividade.domain.events;

import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;

public record RegistroAtividadeCriadoEvent(
    RegistroAtividadeId registroAtividadeId
) {}
