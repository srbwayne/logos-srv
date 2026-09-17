package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionExecutionRequest;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionExecutionReadResponse;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionExecutionHistoryResponse;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionEvaluationResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteIdempotentExternalSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionQuery;
import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionHistoryQuery;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPageRequest;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/v1/progression/executions")
public class ProgressionExecutionController {
    private final GetProgressionExecutionQuery query;
    private final GetProgressionExecutionHistoryQuery historyQuery;
    private final ExecuteIdempotentExternalSubjectProgressionUseCase executionUseCase;

    public ProgressionExecutionController(GetProgressionExecutionQuery query,
                                          GetProgressionExecutionHistoryQuery historyQuery,
                                          ExecuteIdempotentExternalSubjectProgressionUseCase executionUseCase) {
        this.query = query;
        this.historyQuery = historyQuery;
        this.executionUseCase = executionUseCase;
    }

    @PostMapping
    public ResponseEntity<ProgressionEvaluationResponse> create(@RequestBody ProgressionExecutionRequest request) {
        if (request == null || request.subject() == null || request.execution() == null
                || request.configuration() == null || request.details() == null) {
            throw new IllegalArgumentException("subject, execution, configuration and details are required");
        }
        var subject = new ExternalSubjectReference(
                request.subject().namespace(), request.subject().externalId());
        var identity = new ProgressionExecutionIdentity(
                request.execution().source(), request.execution().idempotencyKey());
        var configuration = new ExternalProgressionConfigurationReference(
                request.configuration().key(), request.configuration().revision());
        var facts = new ProgressionFact(request.details().stream()
                .map(detail -> new ProgressionFact.Detail(detail.factorKey(), detail.value()))
                .toList());
        return ResponseEntity.ok(ProgressionEvaluationResponse.from(
                executionUseCase.execute(identity, subject, configuration, facts)));
    }

    @GetMapping
    public ResponseEntity<ProgressionExecutionReadResponse> get(
            @RequestParam String sourceSystem,
            @RequestParam String idempotencyKey) {
        var identity = new ProgressionExecutionIdentity(sourceSystem, idempotencyKey);
        return ResponseEntity.ok(ProgressionExecutionReadResponse.from(query.get(identity)));
    }

    @GetMapping("/history")
    public ResponseEntity<ProgressionExecutionHistoryResponse> history(
            @RequestParam String subjectNamespace,
            @RequestParam String subjectExternalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var subject = new ExternalSubjectReference(subjectNamespace, subjectExternalId);
        var request = new ProgressionExecutionHistoryPageRequest(page, size);
        return ResponseEntity.ok(ProgressionExecutionHistoryResponse.from(historyQuery.get(subject, request)));
    }
}
