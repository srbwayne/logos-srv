package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.out.ActivityProgressionExecutionStore;
import org.springframework.stereotype.Service;

@Service
public class ActivityProgressionRecovery {
    private final ActivityProgressionExecutionStore executions;
    private final ActivityProgressionAdapter adapter;

    public ActivityProgressionRecovery(ActivityProgressionExecutionStore executions,
                                       ActivityProgressionAdapter adapter) {
        this.executions = executions;
        this.adapter = adapter;
    }

    public int recover() {
        int recovered = 0;
        for (var execution : executions.findUnresolved()) {
            adapter.process(java.util.UUID.fromString(execution.identity().idempotencyKey()));
            recovered++;
        }
        return recovered;
    }
}
