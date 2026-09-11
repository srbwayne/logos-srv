package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.api;

import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.CreateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.UpdateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.ReplaceAtividadeFormularioRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.response.AtividadeConfigResponse;
import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.*;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeformulario.application.port.in.GetFormularioByAtividadeIdUseCase;
import com.josecjuniors.logossrv.core.atividadeformulario.application.port.in.ReplaceAtividadeFormularioCommand;
import com.josecjuniors.logossrv.core.atividadeformulario.application.port.in.ReplaceAtividadeFormularioUseCase;
import com.josecjuniors.logossrv.core.atividadeformulario.application.dto.AtividadeFormularioDto;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/atividades-config")
public class AtividadeConfigController {

    private final CreateAtividadeConfigUseCase createUseCase;
    private final UpdateAtividadeConfigUseCase updateUseCase;
    private final GetAtividadeConfigByIdUseCase getByIdUseCase;
    private final GetAllAtividadesConfigUseCase getAllUseCase;
    private final DeleteAtividadeConfigUseCase deleteUseCase;
    private final GetFormularioByAtividadeIdUseCase getFormularioUseCase;
    private final ReplaceAtividadeFormularioUseCase replaceFormularioUseCase;

    public AtividadeConfigController(CreateAtividadeConfigUseCase createUseCase, UpdateAtividadeConfigUseCase updateUseCase, GetAtividadeConfigByIdUseCase getByIdUseCase, GetAllAtividadesConfigUseCase getAllUseCase, DeleteAtividadeConfigUseCase deleteUseCase, GetFormularioByAtividadeIdUseCase getFormularioUseCase, ReplaceAtividadeFormularioUseCase replaceFormularioUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getByIdUseCase = getByIdUseCase;
        this.getAllUseCase = getAllUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getFormularioUseCase = getFormularioUseCase;
        this.replaceFormularioUseCase = replaceFormularioUseCase;
    }

    @PostMapping
    public ResponseEntity<AtividadeConfigResponse> create(@RequestBody CreateAtividadeConfigRequest request) {
        var command = new CreateAtividadeConfigCommand(request.nome(), request.descricao(), request.xpBase(), request.estresseBase(), request.diasParaPenalidade(), request.xpPerdaPorCiclo());
        AtividadeConfigDto dto = createUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(AtividadeConfigResponse.fromDto(dto));
    }

    @GetMapping
    public ResponseEntity<Page<AtividadeConfigResponse>> getAll(
            @RequestParam(value = "searchTerm", defaultValue = "") String searchTerm,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        Page<AtividadeConfigDto> pageDto = getAllUseCase.getAll(searchTerm, pageable);
        return ResponseEntity.ok(pageDto.map(AtividadeConfigResponse::fromDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtividadeConfigResponse> getById(@PathVariable UUID id) {
        return getByIdUseCase.getById(new AtividadeConfigId(id))
                .map(AtividadeConfigResponse::fromDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/formulario")
    public ResponseEntity<AtividadeFormularioJson> getFormulario(@PathVariable UUID id) {
        return getFormularioUseCase.getByAtividadeId(new AtividadeConfigId(id))
                .map(formulario -> ResponseEntity.ok()
                        .header("X-Activity-Form-Version", Integer.toString(formulario.versao()))
                        .body(formulario.formulario()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/formulario")
    public ResponseEntity<AtividadeFormularioJson> replaceFormulario(@PathVariable UUID id,
                                                                       @RequestBody ReplaceAtividadeFormularioRequest request) {
        var campos = request.campos().stream()
                .map(campo -> new ReplaceAtividadeFormularioCommand.Campo(campo.fatorCalculoId(), campo.placeholder()))
                .toList();
        var command = new ReplaceAtividadeFormularioCommand(new AtividadeConfigId(id), request.expectedVersion(), campos);
        AtividadeFormularioDto result = replaceFormularioUseCase.replace(command);
        return ResponseEntity.ok()
                .header("X-Activity-Form-Version", Integer.toString(result.versao()))
                .body(result.formulario());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtividadeConfigResponse> update(@PathVariable UUID id, @RequestBody UpdateAtividadeConfigRequest request) {
        var command = new UpdateAtividadeConfigCommand(new AtividadeConfigId(id), request.nome(), request.descricao(), request.xpBase(), request.estresseBase(), request.diasParaPenalidade(), request.xpPerdaPorCiclo());
        AtividadeConfigDto dto = updateUseCase.update(command);
        return ResponseEntity.ok(AtividadeConfigResponse.fromDto(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUseCase.delete(new AtividadeConfigId(id));
        return ResponseEntity.noContent().build();
    }
}
