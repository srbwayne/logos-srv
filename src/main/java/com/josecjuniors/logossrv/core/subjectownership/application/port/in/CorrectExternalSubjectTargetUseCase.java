package com.josecjuniors.logossrv.core.subjectownership.application.port.in;

import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipTargetCorrectionResult;

public interface CorrectExternalSubjectTargetUseCase {
    SubjectOwnershipTargetCorrectionResult correct(CorrectExternalSubjectTargetCommand command);
}
