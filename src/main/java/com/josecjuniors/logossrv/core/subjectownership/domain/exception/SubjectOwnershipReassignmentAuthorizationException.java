package com.josecjuniors.logossrv.core.subjectownership.domain.exception;

public class SubjectOwnershipReassignmentAuthorizationException extends RuntimeException {
    public SubjectOwnershipReassignmentAuthorizationException() { super("Trusted reassignment authorization is unavailable or does not match"); }
}
