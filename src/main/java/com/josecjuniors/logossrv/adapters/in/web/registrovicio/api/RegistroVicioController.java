package com.josecjuniors.logossrv.adapters.in.web.registrovicio.api;

import com.josecjuniors.logossrv.adapters.in.web.registrovicio.dto.request.CreateRegistroVicioRequest;
import com.josecjuniors.logossrv.adapters.in.web.registrovicio.dto.response.RegistroVicioResponse;
import com.josecjuniors.logossrv.core.registrovicio.application.command.CreateRegistroVicioCommand;
import com.josecjuniors.logossrv.core.registrovicio.application.dto.RegistroVicioDto;
import com.josecjuniors.logossrv.core.registrovicio.application.port.in.CreateRegistroVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/registros-vicio")
public class RegistroVicioController {

    private final CreateRegistroVicioUseCase createUseCase;

    public RegistroVicioController(CreateRegistroVicioUseCase createUseCase) {
        this.createUseCase = createUseCase;
    }

    @PostMapping
    public ResponseEntity<RegistroVicioResponse> create(
            @RequestBody CreateRegistroVicioRequest request,
            Principal principal) {

        String userEmail = principal.getName();

        var command = new CreateRegistroVicioCommand(
                userEmail,
                new VicioId(request.vicioId()),
                request.dataHora(),
                request.observacao()
        );

        RegistroVicioDto dto = createUseCase.create(command);

        // Retornamos 202 Accepted para indicar que a solicitação foi aceita
        // e será processada de forma assíncrona.
        return ResponseEntity.accepted().body(RegistroVicioResponse.fromDto(dto));
    }
}
