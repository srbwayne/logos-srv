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
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationEvaluator;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationEvaluator.AuthorizationDecision;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationSource;
import com.josecjuniors.logossrv.core.security.authorization.application.port.out.AuthorizationGrantStore;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AuthorizationEvaluator authorizationEvaluator;

    @Autowired
    public ProgressionExecutionController(GetProgressionExecutionQuery query,
                                          GetProgressionExecutionHistoryQuery historyQuery,
                                          ExecuteIdempotentExternalSubjectProgressionUseCase executionUseCase,
                                          AuthorizationEvaluator authorizationEvaluator) {
        this.query = query;
        this.historyQuery = historyQuery;
        this.executionUseCase = executionUseCase;
        this.authorizationEvaluator = authorizationEvaluator;
    }

    /** Compatibility constructor for existing query-only controller fixtures; workload grants deny by default. */
    public ProgressionExecutionController(GetProgressionExecutionQuery query,
                                          GetProgressionExecutionHistoryQuery historyQuery,
                                          ExecuteIdempotentExternalSubjectProgressionUseCase executionUseCase) {
        this(query, historyQuery, executionUseCase, new AuthorizationEvaluator(new AuthorizationGrantStore() {
            @Override public InsertResult insertIfAbsent(com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationGrant grant) {
                throw new UnsupportedOperationException("read-only deny store");
            }
            @Override public java.util.Optional<com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationGrant> findExact(
                    com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationGrant grant) {
                return java.util.Optional.empty();
            }
        }));
    }

    @PostMapping
    public ResponseEntity<ProgressionEvaluationResponse> create(@RequestBody ProgressionExecutionRequest request,
                                                                 Authentication authentication) {
        if (request == null || request.subject() == null || request.execution() == null
                || request.configuration() == null || request.details() == null) {
            throw new IllegalArgumentException("subject, execution, configuration and details are required");
        }
        if (request.subject().namespace() == null || request.execution().source() == null) {
            throw new IllegalArgumentException("subject namespace and execution source are required");
        }
        final ExternalSubjectReference subject;
        final ProgressionExecutionIdentity identity;
        try {
            subject = new ExternalSubjectReference(request.subject().namespace(), request.subject().externalId());
            identity = new ProgressionExecutionIdentity(request.execution().source(), request.execution().idempotencyKey());
        } catch (NullPointerException | IllegalArgumentException invalidDimension) {
            throw new IllegalArgumentException("invalid progression subject or execution identity");
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("Forbidden");
        }
        Object authenticatedPrincipal = authentication.getPrincipal();
        if (authenticatedPrincipal instanceof AuthenticatedPrincipal workloadPrincipal) {
            var decision = authorizationEvaluator.evaluate(workloadPrincipal, AuthorizationOperation.PROGRESSION_EXECUTE,
                    java.util.Optional.of(new AuthorizationSource(identity.source())),
                    java.util.Optional.of(new AuthorizationNamespace(subject.namespace())));
            if (decision != AuthorizationDecision.ALLOW) throw new SecurityException("Forbidden");
        } else if (!(authenticatedPrincipal instanceof UserDetails)) {
            throw new SecurityException("Forbidden");
        }
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
