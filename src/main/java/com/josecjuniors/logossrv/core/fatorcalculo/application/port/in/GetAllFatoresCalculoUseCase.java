package com.josecjuniors.logossrv.core.fatorcalculo.application.port.in;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAllFatoresCalculoUseCase {
    Page<FatorCalculoDto> getAll(String searchTerm, Pageable pageable);
}
