package com.josecjuniors.logossrv.core.subjectownership.domain.exception;

public class SubjectOwnershipVersionConflictException extends RuntimeException {
    public SubjectOwnershipVersionConflictException(long expected, long actual) {
        super("Ownership version conflict: expected " + expected + " but current version is " + actual);
    }
}
