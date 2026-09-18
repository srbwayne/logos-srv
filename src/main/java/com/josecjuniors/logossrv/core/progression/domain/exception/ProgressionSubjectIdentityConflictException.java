package com.josecjuniors.logossrv.core.progression.domain.exception;

public class ProgressionSubjectIdentityConflictException extends RuntimeException {
    public ProgressionSubjectIdentityConflictException() {
        super("External subject identity already belongs to another player.");
    }
}
