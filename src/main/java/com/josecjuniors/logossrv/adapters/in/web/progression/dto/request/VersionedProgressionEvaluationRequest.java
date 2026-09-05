package com.josecjuniors.logossrv.adapters.in.web.progression.dto.request;

import java.util.List;

public record VersionedProgressionEvaluationRequest(ConfigurationReference configuration, List<DetailRequest> details) {
    public record ConfigurationReference(String key, Integer revision) {}
    public record DetailRequest(String factorKey, double value) {}
}
