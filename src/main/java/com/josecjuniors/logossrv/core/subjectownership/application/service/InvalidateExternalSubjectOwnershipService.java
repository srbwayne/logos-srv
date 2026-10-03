package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipVerificationEvidence;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvalidateExternalSubjectOwnershipService implements InvalidateExternalSubjectOwnershipUseCase {
    private final SubjectOwnershipOperatorContext operatorContext;
    private final SubjectOwnershipLifecycleStore store;

    public InvalidateExternalSubjectOwnershipService(SubjectOwnershipOperatorContext operatorContext,
            SubjectOwnershipLifecycleStore store) {
        this.operatorContext = operatorContext;
        this.store = store;
    }

    @Override
    @Transactional
    public void invalidate(InvalidateExternalSubjectOwnershipCommand command) {
        var operator = operatorContext.authorizeForNamespace(command.reference().namespace());
        var current = store.findForUpdate(command.reference()).orElseThrow(SubjectOwnershipIdentityNotFoundException::new);
        var evidence = new OwnershipVerificationEvidence(command.evidenceType(), command.evidenceReference(), command.reason());
        if (current.isActiveInvalidated()) return;
        var invalidated = current.invalidate(command.expectedOwnershipVersion());
        store.saveInvalidation(current, invalidated, operator, evidence, Instant.now());
    }
}
