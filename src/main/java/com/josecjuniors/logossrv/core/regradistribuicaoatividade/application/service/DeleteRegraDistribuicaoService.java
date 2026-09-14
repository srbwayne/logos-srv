package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.DeleteRegraDistribuicaoUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import org.springframework.stereotype.Service;
@Service
public class DeleteRegraDistribuicaoService implements DeleteRegraDistribuicaoUseCase {
 @Override public void delete(RegraDistribuicaoAtividadeId id) { throw new LegacyProgressionAuthoringRetiredException(); }
}
