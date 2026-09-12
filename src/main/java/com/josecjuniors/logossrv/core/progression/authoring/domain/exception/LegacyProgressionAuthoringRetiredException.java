package com.josecjuniors.logossrv.core.progression.authoring.domain.exception;

public class LegacyProgressionAuthoringRetiredException extends RuntimeException {
    public LegacyProgressionAuthoringRetiredException() {
        super("Legacy progression authoring has been retired; use modern Progression Configuration authoring.");
    }
}
