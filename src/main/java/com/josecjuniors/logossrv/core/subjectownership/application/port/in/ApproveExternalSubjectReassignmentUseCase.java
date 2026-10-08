package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipReassignmentAuthorization;

public interface ApproveExternalSubjectReassignmentUseCase {
    SubjectOwnershipReassignmentAuthorization approve(ApproveExternalSubjectReassignmentCommand command);
}
