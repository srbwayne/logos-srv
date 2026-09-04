package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionEvaluationRequest;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionEvaluationResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteExternalSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/v1/progression/external")
public class ExternalSubjectProgressionController {

    private final ExecuteExternalSubjectProgressionUseCase progressionUseCase;

    public ExternalSubjectProgressionController(ExecuteExternalSubjectProgressionUseCase progressionUseCase) {
        this.progressionUseCase = progressionUseCase;
    }

    @PostMapping("/{namespace}/{externalId}/evaluate")
    public ResponseEntity<ProgressionEvaluationResponse> evaluate(
            @PathVariable String namespace,
            @PathVariable String externalId,
            @RequestBody ProgressionEvaluationRequest request) {
        if (request == null || request.configurationId() == null || request.details() == null) {
            throw new IllegalStateException("Progression request requires configurationId and details.");
        }
        var fact = new ProgressionFact(request.details().stream()
                .map(detail -> new ProgressionFact.Detail(detail.factorKey(), detail.value()))
                .toList());
        var outcome = progressionUseCase.execute(
                new ExternalSubjectReference(namespace, externalId),
                new ProgressionConfigurationReference(request.configurationId()),
                fact);
        return ResponseEntity.ok(ProgressionEvaluationResponse.from(outcome));
    }
}
