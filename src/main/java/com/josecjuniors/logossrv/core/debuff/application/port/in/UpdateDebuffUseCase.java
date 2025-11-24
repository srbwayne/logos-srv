package com.josecjuniors.logossrv.core.debuff.application.port.in;

import com.josecjuniors.logossrv.core.debuff.application.command.UpdateDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;

public interface UpdateDebuffUseCase {
    DebuffDto update(UpdateDebuffCommand command);
}
