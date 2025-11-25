package com.josecjuniors.logossrv.core.debuff.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoNaoEncontradoException;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.debuff.application.command.CreateRegraDistribuicaoDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.dto.RegraDistribuicaoDebuffDto;
import com.josecjuniors.logossrv.core.debuff.application.port.in.CreateRegraDistribuicaoDebuffUseCase;
import com.josecjuniors.logossrv.core.debuff.domain.exception.DebuffNaoEncontradoException;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import com.josecjuniors.logossrv.core.debuff.domain.repository.RegraDistribuicaoDebuffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateRegraDistribuicaoDebuffService implements CreateRegraDistribuicaoDebuffUseCase {

    private final RegraDistribuicaoDebuffRepository repository;
    private final DebuffRepository debuffRepository;
    private final AtributoRepository atributoRepository;

    public CreateRegraDistribuicaoDebuffService(RegraDistribuicaoDebuffRepository repository, DebuffRepository debuffRepository, AtributoRepository atributoRepository) {
        this.repository = repository;
        this.debuffRepository = debuffRepository;
        this.atributoRepository = atributoRepository;
    }

    @Override
    public RegraDistribuicaoDebuffDto create(CreateRegraDistribuicaoDebuffCommand command) {
        var debuff = debuffRepository.findById(command.debuffId())
                .orElseThrow(DebuffNaoEncontradoException::new);
        var atributo = atributoRepository.findById(command.atributoId())
                .orElseThrow(AtributoNaoEncontradoException::new);

        var novaRegra = new RegraDistribuicaoDebuff(
                RegraDistribuicaoDebuffId.generate(),
                debuff,
                atributo
        );

        var regraSalva = repository.save(novaRegra);
        return RegraDistribuicaoDebuffDto.fromDomain(regraSalva);
    }
}
