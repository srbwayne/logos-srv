package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadAssertionReplayException;
import com.josecjuniors.logossrv.core.security.workload.application.port.out.ReplayConsumeResult;
import com.josecjuniors.logossrv.core.security.workload.application.port.out.WorkloadAssertionReplayStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkloadAssertionReplayGuard {
    private final WorkloadAssertionReplayStore replayStore;

    public WorkloadAssertionReplayGuard(WorkloadAssertionReplayStore replayStore) {
        this.replayStore = replayStore;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consume(VerifiedWorkloadAssertion assertion) {
        ReplayConsumeResult result = replayStore.consume(
                assertion.issuer(), assertion.jti(), assertion.expiresAt());
        if (result == ReplayConsumeResult.REPLAY) {
            throw new WorkloadAssertionReplayException();
        }
        if (result != ReplayConsumeResult.FIRST_USE) {
            throw new IllegalStateException("Replay store returned an unsupported result");
        }
    }
}
