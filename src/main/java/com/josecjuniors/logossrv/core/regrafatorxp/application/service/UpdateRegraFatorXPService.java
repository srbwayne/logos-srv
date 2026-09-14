package com.josecjuniors.logossrv.core.regrafatorxp.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regrafatorxp.application.dto.RegraFatorXPDto;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.UpdateRegraFatorXPCommand;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.UpdateRegraFatorXPUseCase;
import org.springframework.stereotype.Service;
@Service public class UpdateRegraFatorXPService implements UpdateRegraFatorXPUseCase {
 @Override public RegraFatorXPDto update(UpdateRegraFatorXPCommand command) { throw new LegacyProgressionAuthoringRetiredException(); }
}
