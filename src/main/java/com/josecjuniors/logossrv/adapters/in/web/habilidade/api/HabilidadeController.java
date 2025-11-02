package com.josecjuniors.logossrv.adapters.in.web.habilidade.api;

import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.CreateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.UpdateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.response.HabilidadeResponse;
import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.CreateHabilidadeCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.CreateHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetAllHabilidadesUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.UpdateHabilidadeCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.UpdateHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/habilidades")
public class HabilidadeController {

    private final CreateHabilidadeUseCase createHabilidadeUseCase;
    private final GetAllHabilidadesUseCase getAllHabilidadesUseCase;
    private final UpdateHabilidadeUseCase updateHabilidadeUseCase;

    public HabilidadeController(CreateHabilidadeUseCase createHabilidadeUseCase, GetAllHabilidadesUseCase getAllHabilidadesUseCase, UpdateHabilidadeUseCase updateHabilidadeUseCase) {
        this.createHabilidadeUseCase = createHabilidadeUseCase;
        this.getAllHabilidadesUseCase = getAllHabilidadesUseCase;
        this.updateHabilidadeUseCase = updateHabilidadeUseCase;
    }

    @PostMapping
    public ResponseEntity<HabilidadeResponse> createHabilidade(@RequestBody CreateHabilidadeRequest request) {
        CreateHabilidadeCommand command = new CreateHabilidadeCommand(request.nome());
        HabilidadeDto habilidadeDto = createHabilidadeUseCase.createHabilidade(command);
        return ResponseEntity.ok(toResponse(habilidadeDto));
    }

    @GetMapping
    public ResponseEntity<List<HabilidadeResponse>> getAllHabilidades() {
        List<HabilidadeDto> habilidadeDtos = getAllHabilidadesUseCase.getAllHabilidades();
        List<HabilidadeResponse> response = habilidadeDtos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabilidadeResponse> updateHabilidade(
            @PathVariable UUID id,
            @RequestBody UpdateHabilidadeRequest request) {

        UpdateHabilidadeCommand command = new UpdateHabilidadeCommand(
                new HabilidadeId(id),
                request.nome()
        );

        HabilidadeDto habilidadeDto = updateHabilidadeUseCase.updateHabilidade(command);
        return ResponseEntity.ok(toResponse(habilidadeDto));
    }

    private HabilidadeResponse toResponse(HabilidadeDto dto) {
        return new HabilidadeResponse(dto.id(), dto.nome());
    }
}
