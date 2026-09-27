package com.josecjuniors.logossrv.core.security.workload.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "logos.security.workload.replay-cleanup.enabled",
        havingValue = "true", matchIfMissing = true)
public class WorkloadAssertionReplayCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(WorkloadAssertionReplayCleanupScheduler.class);
    private final WorkloadAssertionReplayCleanup cleanup;
    private final int batchSize;

    public WorkloadAssertionReplayCleanupScheduler(WorkloadAssertionReplayCleanup cleanup,
                                                   @Value("${logos.security.workload.replay-cleanup.batch-size:1000}") int batchSize) {
        this.cleanup = cleanup;
        if (batchSize <= 0) throw new IllegalArgumentException("Replay cleanup batch size must be positive");
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${logos.security.workload.replay-cleanup.fixed-delay:60000}",
            initialDelayString = "${logos.security.workload.replay-cleanup.initial-delay:60000}")
    public void cleanupScheduled() {
        try {
            int deleted = cleanup.cleanupExpired(batchSize);
            if (deleted > 0) log.info("Expired workload replay records cleaned: {} row(s)", deleted);
        } catch (RuntimeException exception) {
            log.error("Workload replay cleanup failed; it will be retried on the next run");
        }
    }
}
