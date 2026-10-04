package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipVerificationEvidence;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAggregate;
import java.time.Instant;
import java.util.Optional;

public interface SubjectOwnershipLifecycleStore {
    Optional<SubjectOwnershipAggregate> findForUpdate(ExternalSubjectReference reference);
    void saveVerification(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, OwnershipVerificationEvidence evidence, Instant effectiveAt);
    void saveInvalidation(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, OwnershipVerificationEvidence evidence, Instant effectiveAt);
    void saveReverification(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, OwnershipVerificationEvidence evidence, Instant effectiveAt);
}
