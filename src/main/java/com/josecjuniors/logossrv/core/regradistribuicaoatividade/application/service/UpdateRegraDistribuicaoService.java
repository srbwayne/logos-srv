package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.UpdateRegraDistribuicaoCommand;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.UpdateRegraDistribuicaoUseCase;
import org.springframework.stereotype.Service;
@Service
public class UpdateRegraDistribuicaoService implements UpdateRegraDistribuicaoUseCase {
 @Override public RegraDistribuicaoAtividadeDto update(UpdateRegraDistribuicaoCommand command) { throw new LegacyProgressionAuthoringRetiredException(); }
}
