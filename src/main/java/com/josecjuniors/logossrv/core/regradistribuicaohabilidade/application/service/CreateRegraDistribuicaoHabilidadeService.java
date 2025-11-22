package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoNaoEncontradoException;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeNaoEncontradaException;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.CreateRegraDistribuicaoHabilidadeCommand;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.dto.RegraDistribuicaoHabilidadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in.CreateRegraDistribuicaoHabilidadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidade;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.repository.RegraDistribuicaoHabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateRegraDistribuicaoHabilidadeService implements CreateRegraDistribuicaoHabilidadeUseCase {

    private final RegraDistribuicaoHabilidadeRepository repository;
    private final HabilidadeRepository habilidadeRepository;
    private final AtributoRepository atributoRepository;

    public CreateRegraDistribuicaoHabilidadeService(RegraDistribuicaoHabilidadeRepository repository, HabilidadeRepository habilidadeRepository, AtributoRepository atributoRepository) {
        this.repository = repository;
        this.habilidadeRepository = habilidadeRepository;
        this.atributoRepository = atributoRepository;
    }

    @Override
    public RegraDistribuicaoHabilidadeDto create(CreateRegraDistribuicaoHabilidadeCommand command) {
        var habilidade = habilidadeRepository.findById(command.habilidadeId())
                .orElseThrow(HabilidadeNaoEncontradaException::new);
        var atributo = atributoRepository.findById(command.atributoId())
                .orElseThrow(AtributoNaoEncontradoException::new);

        var novaRegra = new RegraDistribuicaoHabilidade(
                RegraDistribuicaoHabilidadeId.generate(),
                habilidade,
                atributo,
                command.pesoDistribuicao()
        );

        var regraSalva = repository.save(novaRegra);
        return RegraDistribuicaoHabilidadeDto.fromDomain(regraSalva);
    }
}
