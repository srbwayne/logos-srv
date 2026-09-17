package com.josecjuniors.logossrv.adapters.in.web.progression.dto.request;

import java.util.List;

public record ProgressionExecutionRequest(
        SubjectReference subject,
        ExecutionIdentity execution,
        ConfigurationReference configuration,
        List<DetailRequest> details) {

    public record SubjectReference(String namespace, String externalId) {}

    public record ExecutionIdentity(String source, String idempotencyKey) {}

    public record ConfigurationReference(String key, Integer revision) {}

    public record DetailRequest(String factorKey, double value) {}
}
