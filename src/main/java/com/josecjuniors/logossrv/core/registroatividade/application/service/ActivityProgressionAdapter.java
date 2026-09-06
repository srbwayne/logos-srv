package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.out.ActivityProgressionExecutionStore;
import com.josecjuniors.logossrv.core.progression.application.service.ConfiguredStatefulProgressionApplicationService;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivityProgressionAdapter {
    public static final String SOURCE = "logos.activity";
    private final ActivityProgressionExecutionStore executions;
    private final ConfiguredStatefulProgressionApplicationService progression;
    private final RegistroAtividadeRepository registros;

    public ActivityProgressionAdapter(ActivityProgressionExecutionStore executions,
                                      ConfiguredStatefulProgressionApplicationService progression,
                                      RegistroAtividadeRepository registros) {
        this.executions = executions;
        this.progression = progression;
        this.registros = registros;
    }

    @Transactional
    public boolean process(UUID registroId) {
        var identity = identity(registroId);
        var executionOptional = executions.findActivityForUpdate(identity);
        if (executionOptional.isEmpty()) return false;
        var execution = executionOptional.get();
        if ("COMPLETED".equals(execution.status())) return true;
        var outcome = progression.executeResolved(new SubjectId(execution.subjectId()), execution.resolved(), execution.fact());
        executions.complete(identity, outcome);
        var registro = registros.findByIdWithDetails(new RegistroAtividadeId(registroId)).orElseThrow();
        registro.marcarComoProcessado((int) outcome.result().xpGlobal(), (int) outcome.result().stressTotal(),
                execution.resolved().configurationVersionId(), execution.resolved().skillPolicyVersionId());
        registros.save(registro);
        return true;
    }

    public static ProgressionExecutionIdentity identity(UUID registroId) {
        return new ProgressionExecutionIdentity(SOURCE, registroId.toString());
    }
}
