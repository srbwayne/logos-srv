package com.josecjuniors.logossrv.adapters.in.web.progression.dto.response;

import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryItem;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPage;

import java.time.Instant;
import java.util.List;

public record ProgressionExecutionHistoryResponse(
        List<ItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static ProgressionExecutionHistoryResponse from(ProgressionExecutionHistoryPage history) {
        return new ProgressionExecutionHistoryResponse(
                history.items().stream().map(ItemResponse::from).toList(),
                history.page(), history.size(), history.totalElements(), history.totalPages(), history.hasNext());
    }

    public record ItemResponse(
            ProgressionExecutionReadResponse.IdentityResponse identity,
            ProgressionExecutionReadResponse.SubjectResponse subject,
            String configurationKey,
            Integer requestedRevision,
            java.util.UUID configurationVersionId,
            java.util.UUID skillPolicyVersionId,
            String status,
            ProgressionEvaluationResponse outcome,
            Instant occurredAt) {

        static ItemResponse from(ProgressionExecutionHistoryItem item) {
            var read = item.execution();
            var response = ProgressionExecutionReadResponse.from(read);
            return new ItemResponse(response.identity(), response.subject(), response.configurationKey(),
                    response.requestedRevision(), response.configurationVersionId(), response.skillPolicyVersionId(),
                    response.status(), response.outcome(), item.occurredAt());
        }
    }
}
