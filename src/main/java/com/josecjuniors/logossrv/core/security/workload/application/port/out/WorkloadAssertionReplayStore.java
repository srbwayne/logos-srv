package com.josecjuniors.logossrv.core.security.workload.application.port.out;

import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;

import java.time.Instant;
import java.util.UUID;

public interface WorkloadAssertionReplayStore {
    ReplayConsumeResult consume(WorkloadIssuer issuer, UUID jti, Instant expiresAt);

    int deleteExpiredBefore(Instant cutoff, int limit);
}
