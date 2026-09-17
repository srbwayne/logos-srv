package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionExecutionReadResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionQuery;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/v3/progression/executions")
public class ProgressionExecutionController {
    private final GetProgressionExecutionQuery query;

    public ProgressionExecutionController(GetProgressionExecutionQuery query) {
        this.query = query;
    }

    @GetMapping
    public ResponseEntity<ProgressionExecutionReadResponse> get(
            @RequestParam String sourceSystem,
            @RequestParam String idempotencyKey) {
        var identity = new ProgressionExecutionIdentity(sourceSystem, idempotencyKey);
        return ResponseEntity.ok(ProgressionExecutionReadResponse.from(query.get(identity)));
    }
}
