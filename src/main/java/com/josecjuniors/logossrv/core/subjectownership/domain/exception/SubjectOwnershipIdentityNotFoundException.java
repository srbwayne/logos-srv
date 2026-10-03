package com.josecjuniors.logossrv.core.subjectownership.domain.exception;

public class SubjectOwnershipIdentityNotFoundException extends RuntimeException {
    public SubjectOwnershipIdentityNotFoundException() { super("External subject identity was not found"); }
}
