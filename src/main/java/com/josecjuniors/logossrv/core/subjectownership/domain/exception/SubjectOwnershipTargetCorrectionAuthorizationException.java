package com.josecjuniors.logossrv.core.subjectownership.domain.exception;

public class SubjectOwnershipTargetCorrectionAuthorizationException extends RuntimeException {
    public SubjectOwnershipTargetCorrectionAuthorizationException() {
        super("Target correction authorization is unavailable or does not match");
    }
}
