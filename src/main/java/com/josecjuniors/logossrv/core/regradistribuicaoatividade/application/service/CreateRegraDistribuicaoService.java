package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.CreateRegraDistribuicaoCommand;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.CreateRegraDistribuicaoUseCase;
import org.springframework.stereotype.Service;
@Service
public class CreateRegraDistribuicaoService implements CreateRegraDistribuicaoUseCase {
    @Override public RegraDistribuicaoAtividadeDto create(CreateRegraDistribuicaoCommand command) { throw new LegacyProgressionAuthoringRetiredException(); }
}
