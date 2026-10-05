package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReactivateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReactivateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAdministrativeReason;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactivateExternalSubjectOwnershipService implements ReactivateExternalSubjectOwnershipUseCase {
    private final SubjectOwnershipOperatorContext operatorContext;
    private final SubjectOwnershipLifecycleStore store;

    public ReactivateExternalSubjectOwnershipService(SubjectOwnershipOperatorContext operatorContext,
            SubjectOwnershipLifecycleStore store) {
        this.operatorContext = operatorContext;
        this.store = store;
    }

    @Override
    @Transactional
    public void reactivate(ReactivateExternalSubjectOwnershipCommand command) {
        var operator = operatorContext.authorizeForNamespace(command.reference().namespace());
        var current = store.findForUpdate(command.reference())
                .orElseThrow(SubjectOwnershipIdentityNotFoundException::new);
        var reason = new SubjectOwnershipAdministrativeReason(command.reason());
        if (current.isExternalActiveWithSupportedVerification()) return;
        var reactivated = current.reactivate(command.expectedOwnershipVersion());
        store.saveReactivate(current, reactivated, operator, reason, Instant.now());
    }
}
