package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.events.SkillPolicySalvaEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class SkillPolicyVersionedConfigurationEventListener {
    private final VersionedProgressionConfigurationStore store;

    SkillPolicyVersionedConfigurationEventListener(VersionedProgressionConfigurationStore store) {
        this.store = store;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSkillPolicySaved(SkillPolicySalvaEvent ignored) {
        store.snapshotSkillPolicy();
    }
}
