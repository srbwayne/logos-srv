package com.josecjuniors.logossrv.adapters.in.web.progression.dto.request;

import java.util.List;

public record IdempotentProgressionEvaluationRequest(
        ExecutionIdentity execution,
        VersionedProgressionEvaluationRequest.ConfigurationReference configuration,
        List<VersionedProgressionEvaluationRequest.DetailRequest> details) {
    public record ExecutionIdentity(String source, String idempotencyKey) {}
}
