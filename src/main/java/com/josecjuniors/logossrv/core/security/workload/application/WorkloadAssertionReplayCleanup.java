package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.workload.application.port.out.WorkloadAssertionReplayStore;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

@Service
public class WorkloadAssertionReplayCleanup {
    private static final Duration ACCEPTED_CLOCK_SKEW = Duration.ofSeconds(15);

    private final WorkloadAssertionReplayStore replayStore;
    private final Clock clock;

    public WorkloadAssertionReplayCleanup(WorkloadAssertionReplayStore replayStore, Clock clock) {
        this.replayStore = replayStore;
        this.clock = clock;
    }

    public int cleanupExpired(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Batch limit must be positive");
        }
        return replayStore.deleteExpiredBefore(clock.instant().minus(ACCEPTED_CLOCK_SKEW), limit);
    }
}
