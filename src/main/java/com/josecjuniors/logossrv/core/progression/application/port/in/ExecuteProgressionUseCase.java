package com.josecjuniors.logossrv.core.progression.application.port.in;

import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionInput;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;

/** Caso de uso interno para calcular e aplicar uma progressão a um estado atual. */
public interface ExecuteProgressionUseCase {

    ProgressionOutcome execute(ProgressionInput input, ProgressionProfile currentProfile);
}
