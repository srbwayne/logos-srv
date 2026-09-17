package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionHistoryQuery;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionExecutionHistoryReadPort;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPage;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPageRequest;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProgressionExecutionHistoryService implements GetProgressionExecutionHistoryQuery {
    private final ProgressionExecutionHistoryReadPort executions;

    public GetProgressionExecutionHistoryService(ProgressionExecutionHistoryReadPort executions) {
        this.executions = executions;
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressionExecutionHistoryPage get(ExternalSubjectReference subject,
                                               ProgressionExecutionHistoryPageRequest request) {
        return executions.find(subject, request);
    }
}
