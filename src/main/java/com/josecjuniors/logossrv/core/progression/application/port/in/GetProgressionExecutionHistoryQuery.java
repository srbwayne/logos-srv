package com.josecjuniors.logossrv.core.progression.application.port.in;

import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPage;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPageRequest;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;

public interface GetProgressionExecutionHistoryQuery {
    ProgressionExecutionHistoryPage get(ExternalSubjectReference subject,
                                        ProgressionExecutionHistoryPageRequest request);
}
