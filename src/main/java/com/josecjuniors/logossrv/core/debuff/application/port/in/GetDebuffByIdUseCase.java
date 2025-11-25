package com.josecjuniors.logossrv.core.debuff.application.port.in;

import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;

import java.util.Optional;

public interface GetDebuffByIdUseCase {
    Optional<DebuffDto> findById(DebuffId id);
}
