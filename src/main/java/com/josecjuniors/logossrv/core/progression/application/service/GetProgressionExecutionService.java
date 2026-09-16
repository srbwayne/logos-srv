package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionQuery;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionExecutionReadPort;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionRead;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProgressionExecutionService implements GetProgressionExecutionQuery {
    private final ProgressionExecutionReadPort executions;

    public GetProgressionExecutionService(ProgressionExecutionReadPort executions) {
        this.executions = executions;
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressionExecutionRead get(ProgressionExecutionIdentity identity) {
        return executions.find(identity).orElseThrow(ProgressionExecutionNotFoundException::new);
    }
}
