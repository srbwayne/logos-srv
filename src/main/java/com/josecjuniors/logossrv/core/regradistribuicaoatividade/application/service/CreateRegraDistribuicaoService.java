package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoNaoEncontradoException;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.CreateRegraDistribuicaoCommand;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.CreateRegraDistribuicaoUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateRegraDistribuicaoService implements CreateRegraDistribuicaoUseCase {

    private final RegraDistribuicaoAtividadeRepository regraRepository;
    private final AtividadeConfigRepository atividadeConfigRepository;
    private final AtributoRepository atributoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateRegraDistribuicaoService(RegraDistribuicaoAtividadeRepository regraRepository, AtividadeConfigRepository atividadeConfigRepository, AtributoRepository atributoRepository, ApplicationEventPublisher eventPublisher) {
        this.regraRepository = regraRepository;
        this.atividadeConfigRepository = atividadeConfigRepository;
        this.atributoRepository = atributoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RegraDistribuicaoAtividadeDto create(CreateRegraDistribuicaoCommand command) {
        AtividadeConfig atividade = atividadeConfigRepository.findById(command.atividadeConfigId())
                .orElseThrow(AtividadeConfigNaoEncontradaException::new);
        Atributo atributo = atributoRepository.findById(command.atributoId())
                .orElseThrow(AtributoNaoEncontradoException::new);

        RegraDistribuicaoAtividade novaRegra = new RegraDistribuicaoAtividade(
                new RegraDistribuicaoAtividadeId(),
                atividade,
                atributo,
                command.pesoPercentual()
        );

        RegraDistribuicaoAtividade regraSalva = regraRepository.save(novaRegra);

        // Publica o evento para que o formulário da AtividadeConfig seja regenerado
        eventPublisher.publishEvent(new AtividadeConfigSalvaEvent(atividade.getId()));

        return RegraDistribuicaoAtividadeDto.fromDomain(regraSalva);
    }
}
