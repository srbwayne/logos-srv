package com.josecjuniors.logossrv.core.registroatividade.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityProgressionRecoverySchedulerTest {
    @Test
    void scheduledInvocationDelegatesToRecoveryUseCase() {
        var recovery = mock(ActivityProgressionRecovery.class);
        when(recovery.recover()).thenReturn(3);
        var scheduler = new ActivityProgressionRecoveryScheduler(recovery);

        scheduler.recoverScheduled();

        verify(recovery).recover();
    }

    @Test
    void recoveryFailureDoesNotEscapeScheduledBoundary() {
        var recovery = mock(ActivityProgressionRecovery.class);
        when(recovery.recover()).thenThrow(new IllegalStateException("controlled failure"));
        var scheduler = new ActivityProgressionRecoveryScheduler(recovery);

        scheduler.recoverScheduled();

        assertThat(scheduler).isNotNull();
        verify(recovery).recover();
    }
}
