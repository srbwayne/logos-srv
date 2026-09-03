package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteProgressionUseCase;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionEngine;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionInput;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;
import org.springframework.stereotype.Service;

/** Coordena o cálculo e a aplicação da progressão sem conhecer sua origem ou persistência. */
@Service
public class ProgressionApplicationService implements ExecuteProgressionUseCase {

    private final ProgressionEngine progressionEngine;

    public ProgressionApplicationService() {
        this(new ProgressionEngine());
    }

    public ProgressionApplicationService(ProgressionEngine progressionEngine) {
        this.progressionEngine = progressionEngine;
    }

    @Override
    public ProgressionOutcome execute(ProgressionInput input, ProgressionProfile currentProfile) {
        ProgressionResult result = progressionEngine.calculate(input);
        return new ProgressionOutcome(result, currentProfile.apply(result));
    }
}
