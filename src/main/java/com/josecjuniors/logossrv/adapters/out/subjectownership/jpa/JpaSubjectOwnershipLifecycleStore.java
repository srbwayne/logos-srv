package com.josecjuniors.logossrv.adapters.out.subjectownership.jpa;

import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentity;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentityJpaRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipVerificationEvidence;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAggregate;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubjectOwnershipLifecycleStore implements SubjectOwnershipLifecycleStore {
    private final ProgressionSubjectIdentityJpaRepository identities;
    private final ProgressionSubjectOwnershipHistoryJpaRepository history;

    public JpaSubjectOwnershipLifecycleStore(ProgressionSubjectIdentityJpaRepository identities,
            ProgressionSubjectOwnershipHistoryJpaRepository history) {
        this.identities = identities;
        this.history = history;
    }

    @Override
    public Optional<SubjectOwnershipAggregate> findForUpdate(ExternalSubjectReference reference) {
        return identities.findByNamespaceAndExternalIdForUpdate(reference.namespace(), reference.externalId())
                .map(JpaSubjectOwnershipLifecycleStore::toAggregate);
    }

    @Override
    public void saveVerification(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, OwnershipVerificationEvidence evidence, Instant effectiveAt) {
        ProgressionSubjectIdentity identity = identities.findById(before.id()).orElseThrow();
        identity.applyVerification(after.verificationStatus(), after.ownershipVersion());
        history.saveAndFlush(ProgressionSubjectOwnershipHistoryEntry.verified(before, after, operator, evidence, effectiveAt));
    }

    @Override
    public void saveInvalidation(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, OwnershipVerificationEvidence evidence, Instant effectiveAt) {
        ProgressionSubjectIdentity identity = identities.findById(before.id()).orElseThrow();
        identity.applyVerification(after.verificationStatus(), after.ownershipVersion());
        history.saveAndFlush(ProgressionSubjectOwnershipHistoryEntry.invalidated(before, after, operator, evidence, effectiveAt));
    }

    @Override
    public void saveReverification(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, OwnershipVerificationEvidence evidence, Instant effectiveAt) {
        ProgressionSubjectIdentity identity = identities.findById(before.id()).orElseThrow();
        identity.applyVerification(after.verificationStatus(), after.ownershipVersion());
        history.saveAndFlush(ProgressionSubjectOwnershipHistoryEntry.reverified(before, after, operator, evidence, effectiveAt));
    }

    private static SubjectOwnershipAggregate toAggregate(ProgressionSubjectIdentity identity) {
        return new SubjectOwnershipAggregate(identity.getId(),
                new ExternalSubjectReference(identity.getNamespace(), identity.getExternalId()),
                identity.getJogador().getId().getValue(), identity.getIdentityClass(), identity.getOwnershipStatus(),
                identity.getVerificationStatus(), identity.getOwnershipVersion());
    }
}
