package com.josecjuniors.logossrv.core.progression.application.query;

import java.time.Instant;

public record ProgressionExecutionHistoryItem(
        ProgressionExecutionRead execution,
        Instant occurredAt) {
}
