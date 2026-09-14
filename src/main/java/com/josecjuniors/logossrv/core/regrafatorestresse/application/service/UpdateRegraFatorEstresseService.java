package com.josecjuniors.logossrv.core.regrafatorestresse.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.dto.RegraFatorEstresseDto;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.UpdateRegraFatorEstresseCommand;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.UpdateRegraFatorEstresseUseCase;
import org.springframework.stereotype.Service;
@Service public class UpdateRegraFatorEstresseService implements UpdateRegraFatorEstresseUseCase {
 @Override public RegraFatorEstresseDto update(UpdateRegraFatorEstresseCommand command) { throw new LegacyProgressionAuthoringRetiredException(); }
}
