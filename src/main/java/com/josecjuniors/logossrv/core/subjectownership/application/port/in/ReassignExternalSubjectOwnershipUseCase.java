package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentResult;

public interface ReassignExternalSubjectOwnershipUseCase {
    SubjectOwnershipReassignmentResult reassign(ReassignExternalSubjectOwnershipCommand command);
}
