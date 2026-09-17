package com.josecjuniors.logossrv.core.progression.application.query;

import java.util.List;

public record ProgressionExecutionHistoryPage(
        List<ProgressionExecutionHistoryItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {
}
