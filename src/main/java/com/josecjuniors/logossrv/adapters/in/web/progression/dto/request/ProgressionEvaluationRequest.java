package com.josecjuniors.logossrv.adapters.in.web.progression.dto.request;

import java.util.List;
import java.util.UUID;

public record ProgressionEvaluationRequest(UUID configurationId, List<DetailRequest> details) {

    public record DetailRequest(String factorKey, double value) {
    }
}
