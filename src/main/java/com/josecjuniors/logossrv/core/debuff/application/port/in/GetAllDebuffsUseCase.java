package com.josecjuniors.logossrv.core.debuff.application.port.in;

import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAllDebuffsUseCase {
    Page<DebuffDto> getAll(String searchTerm, Pageable pageable);
}
