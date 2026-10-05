package com.josecjuniors.logossrv.core.subjectownership.domain.model;

public record SubjectOwnershipAdministrativeReason(String value) {
    public SubjectOwnershipAdministrativeReason {
        if (value == null) throw new IllegalArgumentException("reason is required");
        value = value.trim();
        if (value.isEmpty() || value.length() > 512)
            throw new IllegalArgumentException("reason must be nonblank and at most 512 characters");
    }
}
