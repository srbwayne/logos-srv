package com.josecjuniors.logossrv.core.atividadeconfig.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.DeleteAtividadeConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigComHistoricoProgressaoException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteAtividadeConfigService implements DeleteAtividadeConfigUseCase {

    private final AtividadeConfigRepository repository;
    private final RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository;

    public DeleteAtividadeConfigService(AtividadeConfigRepository repository, RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository) {
        this.repository = repository;
        this.regraDistribuicaoRepository = regraDistribuicaoRepository;
    }

    @Override
    public void delete(AtividadeConfigId id) {
        var atividade = repository.findById(id).orElseThrow(AtividadeConfigNaoEncontradaException::new);
        if (!regraDistribuicaoRepository.findByAtividadeConfigId(id).isEmpty()) {
            throw new AtividadeConfigComHistoricoProgressaoException();
        }
        repository.deleteById(id);
    }
}
