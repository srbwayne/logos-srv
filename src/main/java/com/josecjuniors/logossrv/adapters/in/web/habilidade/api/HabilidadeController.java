package com.josecjuniors.logossrv.adapters.in.web.habilidade.api;

import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.CreateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.UpdateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.response.GetHabilidadeRequisitoByIdResponse;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.response.HabilidadeRequisitoResponse;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.response.HabilidadeResponse;
import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeDto;
import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeRequisitoDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.CreateHabilidadeCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.CreateHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetAllHabilidadesUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetHabilidadeRequisitoByIdUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetRequisitosDaHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.UpdateHabilidadeCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.UpdateHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisitoId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/habilidades")
public class HabilidadeController {

    private final CreateHabilidadeUseCase createHabilidadeUseCase;
    private final GetAllHabilidadesUseCase getAllHabilidadesUseCase;
    private final UpdateHabilidadeUseCase updateHabilidadeUseCase;
    private final GetRequisitosDaHabilidadeUseCase getRequisitosDaHabilidadeUseCase;
    private final GetHabilidadeRequisitoByIdUseCase getHabilidadeRequisitoByIdUseCase;

    public HabilidadeController(CreateHabilidadeUseCase createHabilidadeUseCase, GetAllHabilidadesUseCase getAllHabilidadesUseCase, UpdateHabilidadeUseCase updateHabilidadeUseCase, GetRequisitosDaHabilidadeUseCase getRequisitosDaHabilidadeUseCase, GetHabilidadeRequisitoByIdUseCase getHabilidadeRequisitoByIdUseCase) {
        this.createHabilidadeUseCase = createHabilidadeUseCase;
        this.getAllHabilidadesUseCase = getAllHabilidadesUseCase;
        this.updateHabilidadeUseCase = updateHabilidadeUseCase;
        this.getRequisitosDaHabilidadeUseCase = getRequisitosDaHabilidadeUseCase;
        this.getHabilidadeRequisitoByIdUseCase = getHabilidadeRequisitoByIdUseCase;
    }

    @GetMapping("/{habilidadeId}/requisitos")
    public ResponseEntity<List<HabilidadeRequisitoResponse>> getRequisitos(@PathVariable UUID habilidadeId) {
        List<HabilidadeRequisitoDto> dtos = getRequisitosDaHabilidadeUseCase.getRequisitos(new HabilidadeId(habilidadeId));
        List<HabilidadeRequisitoResponse> response = dtos.stream()
                .map(HabilidadeRequisitoResponse::fromDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{habilidadeId}/requisitos/{requisitoId}")
    public ResponseEntity<GetHabilidadeRequisitoByIdResponse> getRequisitoById(@PathVariable UUID habilidadeId, @PathVariable UUID requisitoId) {
        return getHabilidadeRequisitoByIdUseCase.getById(new HabilidadeRequisitoId(requisitoId))
                .map(GetHabilidadeRequisitoByIdResponse::fromDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<HabilidadeResponse> createHabilidade(@RequestBody CreateHabilidadeRequest request) {
        CreateHabilidadeCommand command = new CreateHabilidadeCommand(request.nome(), request.descricao());
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
                request.nome(),
                request.descricao()
        );

        HabilidadeDto habilidadeDto = updateHabilidadeUseCase.updateHabilidade(command);
        return ResponseEntity.ok(toResponse(habilidadeDto));
    }

    private HabilidadeResponse toResponse(HabilidadeDto dto) {
        return new HabilidadeResponse(dto.id(), dto.nome(), dto.descricao());
    }
}
