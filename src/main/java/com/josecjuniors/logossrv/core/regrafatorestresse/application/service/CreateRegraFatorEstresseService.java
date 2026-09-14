package com.josecjuniors.logossrv.core.regrafatorestresse.application.service;
import com.josecjuniors.logossrv.core.progression.authoring.domain.exception.LegacyProgressionAuthoringRetiredException;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.dto.RegraFatorEstresseDto;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.CreateRegraFatorEstresseCommand;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.CreateRegraFatorEstresseUseCase;
import org.springframework.stereotype.Service;
@Service public class CreateRegraFatorEstresseService implements CreateRegraFatorEstresseUseCase {
 @Override public RegraFatorEstresseDto create(CreateRegraFatorEstresseCommand command) { throw new LegacyProgressionAuthoringRetiredException(); }
}
