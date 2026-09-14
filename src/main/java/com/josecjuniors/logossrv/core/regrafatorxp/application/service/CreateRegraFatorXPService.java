package com.josecjuniors.logossrv.core.regrafatorxp.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regrafatorxp.application.dto.RegraFatorXPDto;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.CreateRegraFatorXPCommand;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.CreateRegraFatorXPUseCase;
import org.springframework.stereotype.Service;
@Service public class CreateRegraFatorXPService implements CreateRegraFatorXPUseCase {
 @Override public RegraFatorXPDto create(CreateRegraFatorXPCommand command) { throw new LegacyProgressionAuthoringRetiredException(); }
}
