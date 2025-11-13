package com.josecjuniors.logossrv.core.regrafatorxp.application.port.in;

import com.josecjuniors.logossrv.core.regrafatorxp.application.dto.RegraFatorXPDto;

public interface CreateRegraFatorXPUseCase {
    RegraFatorXPDto create(CreateRegraFatorXPCommand command);
}
