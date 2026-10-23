package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionAuthorization;

public interface ApproveExternalSubjectTargetCorrectionUseCase {
    SubjectOwnershipTargetCorrectionAuthorization approve(ApproveExternalSubjectTargetCorrectionCommand command);
}
