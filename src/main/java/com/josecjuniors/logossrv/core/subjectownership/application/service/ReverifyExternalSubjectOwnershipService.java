package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReverifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReverifyExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipVerificationEvidence;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReverifyExternalSubjectOwnershipService implements ReverifyExternalSubjectOwnershipUseCase {
    private final SubjectOwnershipOperatorContext operatorContext;
    private final SubjectOwnershipLifecycleStore store;

    public ReverifyExternalSubjectOwnershipService(SubjectOwnershipOperatorContext operatorContext,
            SubjectOwnershipLifecycleStore store) {
        this.operatorContext = operatorContext;
        this.store = store;
    }

    @Override
    @Transactional
    public void reverify(ReverifyExternalSubjectOwnershipCommand command) {
        var operator = operatorContext.authorizeForNamespace(command.reference().namespace());
        var current = store.findForUpdate(command.reference()).orElseThrow(SubjectOwnershipIdentityNotFoundException::new);
        var evidence = new OwnershipVerificationEvidence(command.evidenceType(), command.evidenceReference(), command.reason());
        if (current.isActiveVerified()) return;
        var reverified = current.reverify(command.expectedOwnershipVersion());
        store.saveReverification(current, reverified, operator, evidence, Instant.now());
    }
}
