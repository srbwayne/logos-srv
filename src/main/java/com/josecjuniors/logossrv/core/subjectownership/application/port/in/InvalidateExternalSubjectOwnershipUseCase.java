package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

public interface InvalidateExternalSubjectOwnershipUseCase {
    void invalidate(InvalidateExternalSubjectOwnershipCommand command);
}
