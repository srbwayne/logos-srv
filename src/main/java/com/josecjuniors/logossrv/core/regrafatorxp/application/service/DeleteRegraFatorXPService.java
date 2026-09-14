package com.josecjuniors.logossrv.core.regrafatorxp.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.DeleteRegraFatorXPUseCase;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import org.springframework.stereotype.Service;
@Service public class DeleteRegraFatorXPService implements DeleteRegraFatorXPUseCase {
 @Override public void delete(RegraFatorXPId id) { throw new LegacyProgressionAuthoringRetiredException(); }
}
