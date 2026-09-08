package com.josecjuniors.logossrv.core.registroatividade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "logos.progression.activity-recovery.enabled",
        havingValue = "true", matchIfMissing = true)
public class ActivityProgressionRecoveryScheduler {
    private static final Logger log = LoggerFactory.getLogger(ActivityProgressionRecoveryScheduler.class);

    private final ActivityProgressionRecovery recovery;

    public ActivityProgressionRecoveryScheduler(ActivityProgressionRecovery recovery) {
        this.recovery = recovery;
    }

    @Scheduled(
            fixedDelayString = "${logos.progression.activity-recovery.fixed-delay:30000}",
            initialDelayString = "${logos.progression.activity-recovery.initial-delay:10000}")
    public void recoverScheduled() {
        try {
            int recovered = recovery.recover();
            log.info("Automatic activity progression recovery completed: {} execution(s) processed", recovered);
        } catch (RuntimeException exception) {
            log.error("Automatic activity progression recovery failed; it will be retried on the next run", exception);
        }
    }
}
