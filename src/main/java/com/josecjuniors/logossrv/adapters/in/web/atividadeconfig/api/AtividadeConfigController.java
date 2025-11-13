package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.api;

import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.CreateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request.UpdateAtividadeConfigRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.response.AtividadeConfigResponse;
import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.CreateAtividadeConfigCommand;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.CreateAtividadeConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.DeleteAtividadeConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.GetAllAtividadesConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.GetAtividadeConfigByIdUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.UpdateAtividadeConfigCommand;
import com.josecjuniors.logossrv.core.atividadeconfig.application.port.in.UpdateAtividadeConfigUseCase;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
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
@RequestMapping("/api/atividades-config")
public class AtividadeConfigController {

    private final CreateAtividadeConfigUseCase createUseCase;
    private final UpdateAtividadeConfigUseCase updateUseCase;
    private final GetAtividadeConfigByIdUseCase getByIdUseCase;
    private final GetAllAtividadesConfigUseCase getAllUseCase;
    private final DeleteAtividadeConfigUseCase deleteUseCase;

    public AtividadeConfigController(CreateAtividadeConfigUseCase createUseCase, UpdateAtividadeConfigUseCase updateUseCase, GetAtividadeConfigByIdUseCase getByIdUseCase, GetAllAtividadesConfigUseCase getAllUseCase, DeleteAtividadeConfigUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getByIdUseCase = getByIdUseCase;
        this.getAllUseCase = getAllUseCase;
        this.deleteUseCase = deleteUseCase;
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
