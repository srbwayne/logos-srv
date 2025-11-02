package com.josecjuniors.logossrv.core.appuser.application.port.in;

import com.josecjuniors.logossrv.core.appuser.application.dto.RegistrationResult; 

public interface RegistrationUseCase {
    RegistrationResult register(RegistrationCommand command);
}
