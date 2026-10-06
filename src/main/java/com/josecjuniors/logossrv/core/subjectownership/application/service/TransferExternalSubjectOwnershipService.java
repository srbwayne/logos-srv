package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.subjectownership.application.port.in.TransferExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.TransferExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAdministrativeReason;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipTransferEvidence;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferExternalSubjectOwnershipService implements TransferExternalSubjectOwnershipUseCase {
    private final SubjectOwnershipOperatorContext operatorContext;
    private final SubjectOwnershipLifecycleStore store;

    public TransferExternalSubjectOwnershipService(SubjectOwnershipOperatorContext operatorContext,
            SubjectOwnershipLifecycleStore store) {
        this.operatorContext = operatorContext;
        this.store = store;
    }

    @Override
    @Transactional
    public void transfer(TransferExternalSubjectOwnershipCommand command) {
        var operator = operatorContext.authorizeForNamespace(command.reference().namespace());
        var current = store.findForUpdate(command.reference())
                .orElseThrow(SubjectOwnershipIdentityNotFoundException::new);
        var reason = new SubjectOwnershipAdministrativeReason(command.reason());
        var evidence = new SubjectOwnershipTransferEvidence(command.transferEvidenceReference());

        if (store.isEquivalentTransferReplay(current, command.expectedOwnershipVersion(),
                command.newTargetJogadorId(), evidence, reason)) return;

        current.validateTransfer(command.expectedOwnershipVersion(), command.newTargetJogadorId());
        if (!store.targetExists(command.newTargetJogadorId()))
            throw new InvalidSubjectOwnershipTransitionException("New target jogador does not exist");

        var transferred = current.transfer(command.expectedOwnershipVersion(), command.newTargetJogadorId());
        store.saveTransfer(current, transferred, operator, evidence, reason, Instant.now());
    }
}
