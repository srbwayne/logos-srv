package com.josecjuniors.logossrv.core.atividadeconfig.domain.events;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;

/**
 * Evento publicado quando uma AtividadeConfig é criada ou atualizada.
 */
public record AtividadeConfigSalvaEvent(
    AtividadeConfigId atividadeConfigId
) {}
