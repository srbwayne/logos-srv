package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionSubjectIdentityRequest;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionSubjectIdentityResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.ProvisionCurrentExternalSubjectIdentityUseCase;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/internal/v1/progression/subject-identities")
public class ProgressionSubjectIdentityController {

    private final ProvisionCurrentExternalSubjectIdentityUseCase provisioning;

    public ProgressionSubjectIdentityController(ProvisionCurrentExternalSubjectIdentityUseCase provisioning) {
        this.provisioning = provisioning;
    }

    @PostMapping
    public ResponseEntity<ProgressionSubjectIdentityResponse> provision(
            @RequestBody ProgressionSubjectIdentityRequest request,
            Principal principal) {
        if (request == null || request.namespace() == null || request.externalId() == null) {
            throw new IllegalArgumentException("namespace and externalId are required");
        }
        var reference = new ExternalSubjectReference(request.namespace(), request.externalId());
        return ResponseEntity.ok(ProgressionSubjectIdentityResponse.from(
                provisioning.provision(principal.getName(), reference)));
    }
}
