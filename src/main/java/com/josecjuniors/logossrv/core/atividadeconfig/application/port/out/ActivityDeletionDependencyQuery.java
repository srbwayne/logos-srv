package com.josecjuniors.logossrv.core.atividadeconfig.application.port.out;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;

public interface ActivityDeletionDependencyQuery {
    boolean hasProgressionDefinition(AtividadeConfigId id);
    boolean hasRegistroAtividade(AtividadeConfigId id);
    boolean hasAtividadeAgendada(AtividadeConfigId id);
}
