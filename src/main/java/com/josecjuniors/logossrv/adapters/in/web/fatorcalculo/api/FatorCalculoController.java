package com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.api;

import com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.dto.request.CreateFatorCalculoRequest;
import com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.dto.request.UpdateFatorCalculoRequest;
import com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.dto.request.AssignFatorCalculoSemanticKeyRequest;
import com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.dto.response.FatorCalculoResponse;
import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.CreateFatorCalculoCommand;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.CreateFatorCalculoUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.DeleteFatorCalculoUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.GetAllFatoresCalculoUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.GetFatorCalculoByIdUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.UpdateFatorCalculoCommand;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.UpdateFatorCalculoUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.AssignFatorCalculoSemanticKeyUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/fatores-calculo")
public class FatorCalculoController {

    private final CreateFatorCalculoUseCase createUseCase;
    private final UpdateFatorCalculoUseCase updateUseCase;
    private final GetFatorCalculoByIdUseCase getByIdUseCase;
    private final GetAllFatoresCalculoUseCase getAllUseCase;
    private final DeleteFatorCalculoUseCase deleteUseCase;
    private final AssignFatorCalculoSemanticKeyUseCase assignSemanticKeyUseCase;

    public FatorCalculoController(CreateFatorCalculoUseCase createUseCase, UpdateFatorCalculoUseCase updateUseCase, GetFatorCalculoByIdUseCase getByIdUseCase, GetAllFatoresCalculoUseCase getAllUseCase, DeleteFatorCalculoUseCase deleteUseCase, AssignFatorCalculoSemanticKeyUseCase assignSemanticKeyUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getByIdUseCase = getByIdUseCase;
        this.getAllUseCase = getAllUseCase;
        this.deleteUseCase = deleteUseCase;
        this.assignSemanticKeyUseCase = assignSemanticKeyUseCase;
    }

    @PutMapping("/{id}/semantic-key")
    public ResponseEntity<FatorCalculoResponse> assignSemanticKey(@PathVariable UUID id,
                                                                  @RequestBody AssignFatorCalculoSemanticKeyRequest request) {
        var dto = assignSemanticKeyUseCase.assign(new FatorCalculoId(id), request.semanticKey());
        return ResponseEntity.ok(FatorCalculoResponse.fromDto(dto));
    }

    @PostMapping
    public ResponseEntity<FatorCalculoResponse> create(@RequestBody CreateFatorCalculoRequest request) {
        var command = new CreateFatorCalculoCommand(request.semanticKey(), request.nome(), request.unidadeMedida(), request.tipoInput());
        FatorCalculoDto dto = createUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(FatorCalculoResponse.fromDto(dto));
    }

    @GetMapping
    public ResponseEntity<Page<FatorCalculoResponse>> getAll(
            @RequestParam(value = "searchTerm", defaultValue = "") String searchTerm,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        Page<FatorCalculoDto> pageDto = getAllUseCase.getAll(searchTerm, pageable);
        return ResponseEntity.ok(pageDto.map(FatorCalculoResponse::fromDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FatorCalculoResponse> getById(@PathVariable UUID id) {
        return getByIdUseCase.getById(new FatorCalculoId(id))
                .map(FatorCalculoResponse::fromDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<FatorCalculoResponse> update(@PathVariable UUID id, @RequestBody UpdateFatorCalculoRequest request) {
        var command = new UpdateFatorCalculoCommand(new FatorCalculoId(id), request.nome(), request.unidadeMedida(), request.tipoInput());
        FatorCalculoDto dto = updateUseCase.update(command);
        return ResponseEntity.ok(FatorCalculoResponse.fromDto(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUseCase.delete(new FatorCalculoId(id));
        return ResponseEntity.noContent().build();
    }
}
