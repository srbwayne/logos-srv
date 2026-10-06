package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

public interface RevokeExternalSubjectOwnershipUseCase {
    void revoke(RevokeExternalSubjectOwnershipCommand command);
}
