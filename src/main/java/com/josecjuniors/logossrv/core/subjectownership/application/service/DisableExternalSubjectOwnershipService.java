package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.DisableExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.DisableExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAdministrativeReason;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisableExternalSubjectOwnershipService implements DisableExternalSubjectOwnershipUseCase {
    private final SubjectOwnershipOperatorContext operatorContext;
    private final SubjectOwnershipLifecycleStore store;

    public DisableExternalSubjectOwnershipService(SubjectOwnershipOperatorContext operatorContext,
            SubjectOwnershipLifecycleStore store) {
        this.operatorContext = operatorContext;
        this.store = store;
    }

    @Override
    @Transactional
    public void disable(DisableExternalSubjectOwnershipCommand command) {
        var operator = operatorContext.authorizeForNamespace(command.reference().namespace());
        var current = store.findForUpdate(command.reference())
                .orElseThrow(SubjectOwnershipIdentityNotFoundException::new);
        var reason = new SubjectOwnershipAdministrativeReason(command.reason());
        if (current.isExternalDisabled()) return;
        var disabled = current.disable(command.expectedOwnershipVersion());
        store.saveDisable(current, disabled, operator, reason, Instant.now());
    }
}
