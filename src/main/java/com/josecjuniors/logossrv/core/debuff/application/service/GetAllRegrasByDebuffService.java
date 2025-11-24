package com.josecjuniors.logossrv.core.debuff.application.service;

import com.josecjuniors.logossrv.core.debuff.application.dto.RegraDistribuicaoDebuffDto;
import com.josecjuniors.logossrv.core.debuff.application.port.in.GetAllRegrasByDebuffUseCase;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.repository.RegraDistribuicaoDebuffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAllRegrasByDebuffService implements GetAllRegrasByDebuffUseCase {

    private final RegraDistribuicaoDebuffRepository repository;

    public GetAllRegrasByDebuffService(RegraDistribuicaoDebuffRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<RegraDistribuicaoDebuffDto> getAll(DebuffId debuffId, String searchTerm, Pageable pageable) {
        return repository.findAllByDebuffId(debuffId, searchTerm, pageable)
                .map(RegraDistribuicaoDebuffDto::fromDomain);
    }
}
