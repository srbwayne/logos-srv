package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPage;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPageRequest;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;

public interface ProgressionExecutionHistoryReadPort {
    ProgressionExecutionHistoryPage find(ExternalSubjectReference subject,
                                         ProgressionExecutionHistoryPageRequest request);
}
