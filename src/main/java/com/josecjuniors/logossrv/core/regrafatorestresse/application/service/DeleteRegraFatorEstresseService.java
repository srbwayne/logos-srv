package com.josecjuniors.logossrv.core.regrafatorestresse.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.DeleteRegraFatorEstresseUseCase;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import org.springframework.stereotype.Service;
@Service public class DeleteRegraFatorEstresseService implements DeleteRegraFatorEstresseUseCase {
 @Override public void delete(RegraFatorEstresseId id) { throw new LegacyProgressionAuthoringRetiredException(); }
}
