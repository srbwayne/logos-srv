package com.josecjuniors.logossrv.core.subjectownership.domain.model;

public record OwnershipVerificationEvidence(String evidenceType, String evidenceReference, String reason) {
    public OwnershipVerificationEvidence {
        evidenceType = required(evidenceType, 64, "evidenceType");
        evidenceReference = required(evidenceReference, 255, "evidenceReference");
        reason = required(reason, 512, "reason");
    }

    private static String required(String value, int max, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > max)
            throw new IllegalArgumentException(name + " must be nonblank and at most " + max + " characters");
        return normalized;
    }
}
