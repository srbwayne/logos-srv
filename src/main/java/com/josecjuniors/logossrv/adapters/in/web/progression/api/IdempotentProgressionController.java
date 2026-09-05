package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.IdempotentProgressionEvaluationRequest;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.VersionedProgressionEvaluationRequest;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionEvaluationResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteIdempotentExternalSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/v3/progression/external")
public class IdempotentProgressionController {
    private final ExecuteIdempotentExternalSubjectProgressionUseCase useCase;

    public IdempotentProgressionController(ExecuteIdempotentExternalSubjectProgressionUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/{namespace}/{externalId}/evaluate")
    public ResponseEntity<ProgressionEvaluationResponse> evaluate(
            @PathVariable String namespace, @PathVariable String externalId,
            @RequestBody IdempotentProgressionEvaluationRequest request) {
        if (request == null || request.execution() == null || request.configuration() == null || request.details() == null) {
            throw new IllegalArgumentException("execution, configuration and details are required");
        }
        var identity = new ProgressionExecutionIdentity(request.execution().source(), request.execution().idempotencyKey());
        var subject = new ExternalSubjectReference(namespace, externalId);
        var configuration = new ExternalProgressionConfigurationReference(request.configuration().key(), request.configuration().revision());
        var fact = new ProgressionFact(request.details().stream().map(detail -> new ProgressionFact.Detail(detail.factorKey(), detail.value())).toList());
        return ResponseEntity.ok(ProgressionEvaluationResponse.from(useCase.execute(identity, subject, configuration, fact)));
    }
}
