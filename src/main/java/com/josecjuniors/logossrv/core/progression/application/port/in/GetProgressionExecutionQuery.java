package com.josecjuniors.logossrv.core.progression.application.port.in;

import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionRead;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;

public interface GetProgressionExecutionQuery {
    ProgressionExecutionRead get(ProgressionExecutionIdentity identity);
}
