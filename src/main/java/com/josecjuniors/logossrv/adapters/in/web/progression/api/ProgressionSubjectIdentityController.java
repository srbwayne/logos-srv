package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionSubjectIdentityRequest;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionSubjectIdentityResponse;
import com.josecjuniors.logossrv.adapters.in.web.progression.security.ProgressionIntegrationAccessPolicy;
import com.josecjuniors.logossrv.adapters.in.web.progression.security.ProgressionIntegrationPrincipal;
import com.josecjuniors.logossrv.core.progression.application.port.in.ConfirmExternalSubjectIdentityUseCase;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.security.Principal;

@RestController
@RequestMapping("/api/internal/v1/progression/subject-identities")
public class ProgressionSubjectIdentityController {

    private final ConfirmExternalSubjectIdentityUseCase confirmation;
    private final ProgressionIntegrationAccessPolicy accessPolicy;

    public ProgressionSubjectIdentityController(ConfirmExternalSubjectIdentityUseCase confirmation,
                                                ProgressionIntegrationAccessPolicy accessPolicy) {
        this.confirmation = confirmation;
        this.accessPolicy = accessPolicy;
    }

    @PostMapping
    public ResponseEntity<ProgressionSubjectIdentityResponse> provision(
            @RequestBody ProgressionSubjectIdentityRequest request,
            @AuthenticationPrincipal ProgressionIntegrationPrincipal principal) {
        if (request == null || request.namespace() == null || request.externalId() == null || request.challengeToken() == null) {
            throw new IllegalArgumentException("namespace, externalId and challengeToken are required");
        }
        accessPolicy.authorizeNamespace(principal, request.namespace());
        var reference = new ExternalSubjectReference(request.namespace(), request.externalId());
        return ResponseEntity.ok(ProgressionSubjectIdentityResponse.from(
                confirmation.confirm(reference.namespace(), reference.externalId(), request.challengeToken(), principal.clientId())));
    }
}
