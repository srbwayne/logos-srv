package com.josecjuniors.logossrv.adapters.in.web.habilidade.api;

import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.CreateHabilidadeRequisitoRequest;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.CreateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request.UpdateHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.response.GetHabilidadeRequisitoByIdResponse;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.response.HabilidadeRequisitoResponse;
import com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.response.HabilidadeResponse;
import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeDto;
import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeRequisitoDto;

import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.BuscarHabilidadeRequisitoPorIdCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.CreateHabilidadeCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.CreateHabilidadeRequisitoUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.CreateHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.DeletarHabilidadeRequisitoCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.DeleteHabilidadeRequisitoUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetAllHabilidadesUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetHabilidadeRequisitoByIdUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetRequisitosDaHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.UpdateHabilidadeCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.UpdateHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisitoId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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
    private final CreateHabilidadeRequisitoUseCase createHabilidadeRequisitoUseCase;
    private final DeleteHabilidadeRequisitoUseCase deleteHabilidadeRequisitoUseCase;

    public HabilidadeController(CreateHabilidadeUseCase createHabilidadeUseCase, GetAllHabilidadesUseCase getAllHabilidadesUseCase, UpdateHabilidadeUseCase updateHabilidadeUseCase, GetRequisitosDaHabilidadeUseCase getRequisitosDaHabilidadeUseCase, GetHabilidadeRequisitoByIdUseCase getHabilidadeRequisitoByIdUseCase, CreateHabilidadeRequisitoUseCase createHabilidadeRequisitoUseCase, DeleteHabilidadeRequisitoUseCase deleteHabilidadeRequisitoUseCase) {
        this.createHabilidadeUseCase = createHabilidadeUseCase;
        this.getAllHabilidadesUseCase = getAllHabilidadesUseCase;
        this.updateHabilidadeUseCase = updateHabilidadeUseCase;
        this.getRequisitosDaHabilidadeUseCase = getRequisitosDaHabilidadeUseCase;
        this.getHabilidadeRequisitoByIdUseCase = getHabilidadeRequisitoByIdUseCase;
        this.createHabilidadeRequisitoUseCase = createHabilidadeRequisitoUseCase;
        this.deleteHabilidadeRequisitoUseCase = deleteHabilidadeRequisitoUseCase;
    }

    @PostMapping("/{habilidadeId}/requisitos")
    public ResponseEntity<HabilidadeRequisitoResponse> createRequisito(
            @PathVariable UUID habilidadeId,
            @RequestBody CreateHabilidadeRequisitoRequest request) {
        HabilidadeRequisitoDto dto = createHabilidadeRequisitoUseCase.create(
                habilidadeId,
                request.tipoRequisito(),
                request.requisitoId(),
                request.nivelMinimo()
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(HabilidadeRequisitoResponse.fromDto(dto));
    }

    @DeleteMapping("/{habilidadeId}/requisitos/{requisitoId}")
    public ResponseEntity<Void> deleteRequisito(@PathVariable UUID habilidadeId, @PathVariable UUID requisitoId) {
        deleteHabilidadeRequisitoUseCase.delete(new DeletarHabilidadeRequisitoCommand(new HabilidadeId(habilidadeId), new HabilidadeRequisitoId(requisitoId)));
        return ResponseEntity.noContent().build();
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
        return getHabilidadeRequisitoByIdUseCase.getById(new BuscarHabilidadeRequisitoPorIdCommand(new HabilidadeId(habilidadeId), new HabilidadeRequisitoId(requisitoId)))
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
