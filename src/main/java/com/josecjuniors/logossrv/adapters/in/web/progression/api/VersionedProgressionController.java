package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.VersionedProgressionEvaluationRequest;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionEvaluationResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteConfiguredExternalConfigurationProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteExternalSubjectExternalConfigurationProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/v2/progression")
public class VersionedProgressionController {
    private final ExecuteConfiguredExternalConfigurationProgressionUseCase internal;
    private final ExecuteExternalSubjectExternalConfigurationProgressionUseCase external;

    public VersionedProgressionController(ExecuteConfiguredExternalConfigurationProgressionUseCase internal,
                                          ExecuteExternalSubjectExternalConfigurationProgressionUseCase external) {
        this.internal = internal;
        this.external = external;
    }

    @PostMapping("/{subjectId}/evaluate")
    public ResponseEntity<ProgressionEvaluationResponse> evaluateInternal(@PathVariable UUID subjectId,
                                                                                      @RequestBody VersionedProgressionEvaluationRequest request) {
        var ref = reference(request);
        var outcome = internal.execute(new SubjectId(subjectId), ref, fact(request));
        return ResponseEntity.ok(ProgressionEvaluationResponse.from(outcome));
    }

    @PostMapping("/external/{namespace}/{externalId}/evaluate")
    public ResponseEntity<ProgressionEvaluationResponse> evaluateExternal(@PathVariable String namespace, @PathVariable String externalId,
                                                                                      @RequestBody VersionedProgressionEvaluationRequest request) {
        var ref = reference(request);
        var outcome = external.execute(new ExternalSubjectReference(namespace, externalId), ref, fact(request));
        return ResponseEntity.ok(ProgressionEvaluationResponse.from(outcome));
    }

    private ExternalProgressionConfigurationReference reference(VersionedProgressionEvaluationRequest request) {
        if (request == null || request.configuration() == null || request.details() == null) throw new IllegalArgumentException("configuration and details are required");
        return new ExternalProgressionConfigurationReference(request.configuration().key(), request.configuration().revision());
    }

    private ProgressionFact fact(VersionedProgressionEvaluationRequest request) {
        return new ProgressionFact(request.details().stream().map(d -> new ProgressionFact.Detail(d.factorKey(), d.value())).toList());
    }
}
