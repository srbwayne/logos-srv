package com.josecjuniors.logossrv.adapters.out.subjectownership.jpa;

import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentity;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentityJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.JogadorJpaRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipVerificationEvidence;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAdministrativeReason;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAggregate;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipTransferEvidence;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubjectOwnershipLifecycleStore implements SubjectOwnershipLifecycleStore {
    private final ProgressionSubjectIdentityJpaRepository identities;
    private final ProgressionSubjectOwnershipHistoryJpaRepository history;
    private final JogadorJpaRepository jogadores;

    public JpaSubjectOwnershipLifecycleStore(ProgressionSubjectIdentityJpaRepository identities,
            ProgressionSubjectOwnershipHistoryJpaRepository history, JogadorJpaRepository jogadores) {
        this.identities = identities;
        this.history = history;
        this.jogadores = jogadores;
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

    @Override
    public void saveDisable(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, SubjectOwnershipAdministrativeReason reason, Instant effectiveAt) {
        ProgressionSubjectIdentity identity = identities.findById(before.id()).orElseThrow();
        identity.applyOwnershipStatus(after.ownershipStatus(), after.ownershipVersion());
        history.saveAndFlush(ProgressionSubjectOwnershipHistoryEntry.disabled(before, after, operator, reason, effectiveAt));
    }

    @Override
    public void saveReactivate(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, SubjectOwnershipAdministrativeReason reason, Instant effectiveAt) {
        ProgressionSubjectIdentity identity = identities.findById(before.id()).orElseThrow();
        identity.applyOwnershipStatus(after.ownershipStatus(), after.ownershipVersion());
        history.saveAndFlush(ProgressionSubjectOwnershipHistoryEntry.reactivated(before, after, operator, reason, effectiveAt));
    }

    @Override
    public void saveRevoke(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, SubjectOwnershipAdministrativeReason reason, Instant effectiveAt) {
        ProgressionSubjectIdentity identity = identities.findById(before.id()).orElseThrow();
        identity.applyOwnershipStatus(after.ownershipStatus(), after.ownershipVersion());
        history.saveAndFlush(ProgressionSubjectOwnershipHistoryEntry.revoked(before, after, operator, reason, effectiveAt));
    }

    @Override
    public boolean isEquivalentTransferReplay(SubjectOwnershipAggregate current, long expectedOwnershipVersion,
            java.util.UUID newTargetJogadorId, SubjectOwnershipTransferEvidence evidence,
            SubjectOwnershipAdministrativeReason reason) {
        if (expectedOwnershipVersion == Long.MAX_VALUE) return false;
        return history.findByIdentityIdAndAggregateVersion(current.id(), expectedOwnershipVersion + 1)
                .map(entry -> entry.matchesTransferReplay(current, expectedOwnershipVersion,
                        newTargetJogadorId, evidence, reason))
                .orElse(false);
    }

    @Override
    public boolean targetExists(java.util.UUID jogadorId) {
        return jogadores.existsById(new JogadorId(jogadorId));
    }

    @Override
    public void saveTransfer(SubjectOwnershipAggregate before, SubjectOwnershipAggregate after,
            AuthorizedSubjectOwnershipOperator operator, SubjectOwnershipTransferEvidence evidence,
            SubjectOwnershipAdministrativeReason reason, Instant effectiveAt) {
        ProgressionSubjectIdentity identity = identities.findById(before.id()).orElseThrow();
        var target = jogadores.findById(new JogadorId(after.targetJogadorId()))
                .orElseThrow(() -> new IllegalStateException("Target jogador disappeared before transfer persistence"));
        identity.applyTransfer(target, after.verificationStatus(), after.ownershipVersion());
        history.saveAndFlush(ProgressionSubjectOwnershipHistoryEntry.transferred(
                before, after, operator, evidence, reason, effectiveAt));
    }

    private static SubjectOwnershipAggregate toAggregate(ProgressionSubjectIdentity identity) {
        return new SubjectOwnershipAggregate(identity.getId(),
                new ExternalSubjectReference(identity.getNamespace(), identity.getExternalId()),
                identity.getJogador().getId().getValue(), identity.getIdentityClass(), identity.getOwnershipStatus(),
                identity.getVerificationStatus(), identity.getOwnershipVersion());
    }
}
