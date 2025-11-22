package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.service;

import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.DeletarRegradistribuicaoHabilidadeCommand;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in.DeleteRegraDistribuicaoHabilidadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.exception.RegraDistribuicaoHabilidadeNaoEncontradaException;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.exception.RegradistribuicaoHabilidadeNaoPertenceAEstaHabilidadeException;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.repository.RegraDistribuicaoHabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteRegraDistribuicaoHabilidadeService implements DeleteRegraDistribuicaoHabilidadeUseCase {

    private final RegraDistribuicaoHabilidadeRepository repository;

    public DeleteRegraDistribuicaoHabilidadeService(RegraDistribuicaoHabilidadeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void delete(DeletarRegradistribuicaoHabilidadeCommand command) {
        var regraDistribuicaoHabilidade = repository.findById(command.regraDistribuicaoHabilidadeId())
                .orElseThrow(RegraDistribuicaoHabilidadeNaoEncontradaException::new);

        if (!regraDistribuicaoHabilidade.getHabilidade().getId().equals(command.habilidadeId())) {
            throw new RegradistribuicaoHabilidadeNaoPertenceAEstaHabilidadeException();
        }
        repository.deleteById(regraDistribuicaoHabilidade.getId());
    }
}
