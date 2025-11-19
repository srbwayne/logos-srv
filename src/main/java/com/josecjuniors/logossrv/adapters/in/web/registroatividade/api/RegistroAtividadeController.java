package com.josecjuniors.logossrv.adapters.in.web.registroatividade.api;

import com.josecjuniors.logossrv.adapters.in.web.registroatividade.dto.request.CreateRegistroAtividadeRequest;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeCommand;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/registros-atividade")
public class RegistroAtividadeController {

    private final CreateRegistroAtividadeUseCase createUseCase;

    public RegistroAtividadeController(CreateRegistroAtividadeUseCase createUseCase) {
        this.createUseCase = createUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestBody CreateRegistroAtividadeRequest request,
            Principal principal) {

        String userEmail = principal.getName();

        var command = new CreateRegistroAtividadeCommand(
                userEmail,
                request.atividadeConfigId(),
                request.dataHoraInicio(),
                request.dataHoraFim(),
                request.detalhes()
        );

        createUseCase.create(command);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
