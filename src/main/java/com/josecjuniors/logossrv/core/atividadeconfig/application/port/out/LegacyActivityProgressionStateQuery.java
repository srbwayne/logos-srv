package com.josecjuniors.logossrv.core.atividadeconfig.application.port.out;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;

public interface LegacyActivityProgressionStateQuery {
    boolean existsForActivity(AtividadeConfigId id);
}
