package com.josecjuniors.logossrv.core.debuff.application.port.in;

import com.josecjuniors.logossrv.core.debuff.application.command.CreateDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;

public interface CreateDebuffUseCase {
    DebuffDto create(CreateDebuffCommand command);
}
