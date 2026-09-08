package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.registroatividade.domain.events.RegistroAtividadeCriadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RegistroAtividadeProgressionEventListener {
    private static final Logger log = LoggerFactory.getLogger(RegistroAtividadeProgressionEventListener.class);

    private final ActivityProgressionAdapter adapter;

    public RegistroAtividadeProgressionEventListener(ActivityProgressionAdapter adapter) {
        this.adapter = adapter;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(RegistroAtividadeCriadoEvent event) {
        try {
            if (!adapter.process(event.registroAtividadeId().getValue())) {
                log.debug("No durable activity execution found for {}", event.registroAtividadeId().getValue());
            }
        } catch (RuntimeException exception) {
            log.error("Activity progression processing failed for {}; scheduled recovery will retry it",
                    event.registroAtividadeId().getValue(), exception);
        }
    }
}
