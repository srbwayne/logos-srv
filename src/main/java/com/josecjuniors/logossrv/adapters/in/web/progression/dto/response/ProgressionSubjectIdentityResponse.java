package com.josecjuniors.logossrv.adapters.in.web.progression.dto.response;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;

public record ProgressionSubjectIdentityResponse(String namespace, String externalId) {
    public static ProgressionSubjectIdentityResponse from(ExternalSubjectReference reference) {
        return new ProgressionSubjectIdentityResponse(reference.namespace(), reference.externalId());
    }
}
