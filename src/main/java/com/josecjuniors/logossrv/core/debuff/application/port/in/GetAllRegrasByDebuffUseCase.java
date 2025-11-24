package com.josecjuniors.logossrv.core.debuff.application.port.in;

import com.josecjuniors.logossrv.core.debuff.application.dto.RegraDistribuicaoDebuffDto;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAllRegrasByDebuffUseCase {
    Page<RegraDistribuicaoDebuffDto> getAll(DebuffId debuffId, String searchTerm, Pageable pageable);
}
