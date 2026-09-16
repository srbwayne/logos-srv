package com.josecjuniors.logossrv.adapters.in.web.progression.dto.response;

import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionRead;

public record ProgressionExecutionReadResponse(
        IdentityResponse identity,
        SubjectResponse subject,
        String configurationKey,
        Integer requestedRevision,
        java.util.UUID configurationVersionId,
        java.util.UUID skillPolicyVersionId,
        String status,
        ProgressionEvaluationResponse outcome) {

    public static ProgressionExecutionReadResponse from(ProgressionExecutionRead read) {
        return new ProgressionExecutionReadResponse(
                new IdentityResponse(read.identity().source(), read.identity().idempotencyKey()),
                new SubjectResponse(read.subject().namespace(), read.subject().externalId()),
                read.configurationKey(),
                read.requestedRevision(),
                read.configurationVersionId(),
                read.skillPolicyVersionId(),
                read.status().name(),
                read.outcome() == null ? null : ProgressionEvaluationResponse.from(read.outcome()));
    }

    public record IdentityResponse(String source, String idempotencyKey) {}

    public record SubjectResponse(String namespace, String externalId) {}
}
