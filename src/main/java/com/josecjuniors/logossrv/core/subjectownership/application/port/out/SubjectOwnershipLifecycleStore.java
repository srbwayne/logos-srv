package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipVerificationEvidence;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAdministrativeReason;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAggregate;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipTransferEvidence;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SubjectOwnershipLifecycleStore {
    Optional<SubjectOwnershipAggregate> findForUpdate(ExternalSubjectReference reference);
    void saveVerification(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, OwnershipVerificationEvidence evidence, Instant effectiveAt);
    void saveInvalidation(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, OwnershipVerificationEvidence evidence, Instant effectiveAt);
    void saveReverification(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, OwnershipVerificationEvidence evidence, Instant effectiveAt);
    void saveDisable(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, SubjectOwnershipAdministrativeReason reason, Instant effectiveAt);
    void saveReactivate(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, SubjectOwnershipAdministrativeReason reason, Instant effectiveAt);
    void saveRevoke(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, SubjectOwnershipAdministrativeReason reason, Instant effectiveAt);
    boolean isEquivalentTransferReplay(SubjectOwnershipAggregate current, long expectedOwnershipVersion,
            UUID newTargetJogadorId, SubjectOwnershipTransferEvidence evidence,
            SubjectOwnershipAdministrativeReason reason);
    boolean targetExists(UUID jogadorId);
    void saveTransfer(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, SubjectOwnershipTransferEvidence evidence,
            SubjectOwnershipAdministrativeReason reason, Instant effectiveAt);
}
