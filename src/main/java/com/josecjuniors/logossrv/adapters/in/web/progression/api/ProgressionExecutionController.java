package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionExecutionReadResponse;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionExecutionHistoryResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionQuery;
import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionHistoryQuery;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPageRequest;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/v3/progression/executions")
public class ProgressionExecutionController {
    private final GetProgressionExecutionQuery query;
    private final GetProgressionExecutionHistoryQuery historyQuery;

    @Autowired
    public ProgressionExecutionController(GetProgressionExecutionQuery query,
                                          GetProgressionExecutionHistoryQuery historyQuery) {
        this.query = query;
        this.historyQuery = historyQuery;
    }

    public ProgressionExecutionController(GetProgressionExecutionQuery query) {
        this(query, (subject, request) -> {
            throw new UnsupportedOperationException("history query not configured");
        });
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
