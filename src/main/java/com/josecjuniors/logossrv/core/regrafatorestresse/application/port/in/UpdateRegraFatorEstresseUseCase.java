package com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in;

import com.josecjuniors.logossrv.core.regrafatorestresse.application.dto.RegraFatorEstresseDto;

public interface UpdateRegraFatorEstresseUseCase {
    RegraFatorEstresseDto update(UpdateRegraFatorEstresseCommand command);
}
