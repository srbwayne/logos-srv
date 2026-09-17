package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionRead;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;

import java.util.Optional;

public interface ProgressionExecutionReadPort {
    Optional<ProgressionExecutionRead> find(ProgressionExecutionIdentity identity);
}
