package com.josecjuniors.logossrv.core.subjectownership.domain.model;

public record SubjectOwnershipTransferEvidence(String reference) {
    public static final String TYPE = "BILATERAL_TRANSFER_CONSENT";

    public SubjectOwnershipTransferEvidence {
        if (reference == null) throw new IllegalArgumentException("transferEvidenceReference is required");
        reference = reference.trim();
        if (reference.isEmpty() || reference.length() > 255)
            throw new IllegalArgumentException(
                    "transferEvidenceReference must be nonblank and at most 255 characters");
    }
}
