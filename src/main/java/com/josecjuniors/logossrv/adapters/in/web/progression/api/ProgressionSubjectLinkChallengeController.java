package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionSubjectLinkChallengeRequest;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionSubjectLinkChallengeResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.CreateProgressionSubjectLinkChallengeUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/api/internal/v1/progression/subject-link-challenges")
public class ProgressionSubjectLinkChallengeController {
    private final CreateProgressionSubjectLinkChallengeUseCase creation;

    public ProgressionSubjectLinkChallengeController(CreateProgressionSubjectLinkChallengeUseCase creation) {
        this.creation = creation;
    }

    @PostMapping
    public ResponseEntity<ProgressionSubjectLinkChallengeResponse> create(
            @RequestBody ProgressionSubjectLinkChallengeRequest request, Principal principal) {
        if (request == null || request.namespace() == null) {
            throw new IllegalArgumentException("namespace is required");
        }
        var challenge = creation.create(principal.getName(), request.namespace());
        return ResponseEntity.created(URI.create("/api/internal/v1/progression/subject-link-challenges"))
                .body(ProgressionSubjectLinkChallengeResponse.from(challenge));
    }
}
