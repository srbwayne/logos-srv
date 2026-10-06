package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAdministrativeReason;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevokeExternalSubjectOwnershipService implements RevokeExternalSubjectOwnershipUseCase {
    private final SubjectOwnershipOperatorContext operatorContext;
    private final SubjectOwnershipLifecycleStore store;

    public RevokeExternalSubjectOwnershipService(SubjectOwnershipOperatorContext operatorContext,
            SubjectOwnershipLifecycleStore store) {
        this.operatorContext = operatorContext;
        this.store = store;
    }

    @Override
    @Transactional
    public void revoke(RevokeExternalSubjectOwnershipCommand command) {
        var operator = operatorContext.authorizeForNamespace(command.reference().namespace());
        var current = store.findForUpdate(command.reference())
                .orElseThrow(SubjectOwnershipIdentityNotFoundException::new);
        var reason = new SubjectOwnershipAdministrativeReason(command.reason());
        if (current.isExternalRevokedWithSupportedVerification()) return;
        var revoked = current.revoke(command.expectedOwnershipVersion());
        store.saveRevoke(current, revoked, operator, reason, Instant.now());
    }
}
