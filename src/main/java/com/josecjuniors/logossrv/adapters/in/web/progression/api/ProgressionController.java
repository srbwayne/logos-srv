package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionEvaluationRequest;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionEvaluationResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteConfiguredSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/v1/progression")
public class ProgressionController {

    private final ExecuteConfiguredSubjectProgressionUseCase progressionUseCase;

    public ProgressionController(ExecuteConfiguredSubjectProgressionUseCase progressionUseCase) {
        this.progressionUseCase = progressionUseCase;
    }

    @PostMapping("/{subjectId}/evaluate")
    public ResponseEntity<ProgressionEvaluationResponse> evaluate(
            @PathVariable UUID subjectId,
            @RequestBody ProgressionEvaluationRequest request) {
        if (request == null || request.configurationId() == null || request.details() == null) {
            throw new IllegalStateException("Progression request requires configurationId and details.");
        }
        var fact = new ProgressionFact(request.details().stream()
                .map(detail -> new ProgressionFact.Detail(detail.factorKey(), detail.value()))
                .toList());
        var outcome = progressionUseCase.execute(
                new SubjectId(subjectId),
                new ProgressionConfigurationReference(request.configurationId()),
                fact);
        return ResponseEntity.ok(ProgressionEvaluationResponse.from(outcome));
    }
}
