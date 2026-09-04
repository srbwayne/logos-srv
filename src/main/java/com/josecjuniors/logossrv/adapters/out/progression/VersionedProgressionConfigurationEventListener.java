package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.events.SkillPolicySalvaEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class VersionedProgressionConfigurationEventListener {
    private final VersionedProgressionConfigurationStore store;

    VersionedProgressionConfigurationEventListener(VersionedProgressionConfigurationStore store) {
        this.store = store;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfigurationSaved(AtividadeConfigSalvaEvent event) {
        store.snapshotConfiguration(event.atividadeConfigId().getValue());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSkillPolicySaved(SkillPolicySalvaEvent ignored) {
        store.snapshotSkillPolicy();
    }
}
