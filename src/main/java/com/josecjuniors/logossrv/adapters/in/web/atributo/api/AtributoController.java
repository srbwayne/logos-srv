package com.josecjuniors.logossrv.adapters.in.web.atributo.api;

import com.josecjuniors.logossrv.adapters.in.web.atributo.dto.request.CreateAtributoRequest;
import com.josecjuniors.logossrv.adapters.in.web.atributo.dto.request.UpdateAtributoRequest;
import com.josecjuniors.logossrv.adapters.in.web.atributo.dto.response.AtributoResponse;
import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;
import com.josecjuniors.logossrv.core.atributo.application.port.in.*;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/atributos")
public class AtributoController {

    private final CreateAtributoUseCase createAtributoUseCase;
    private final GetAtributoByIdUseCase getAtributoByIdUseCase;
    private final UpdateAtributoUseCase updateAtributoUseCase;
    private final GetAllAtributosUseCase getAllAtributosUseCase;

    public AtributoController(CreateAtributoUseCase createAtributoUseCase, GetAtributoByIdUseCase getAtributoByIdUseCase, UpdateAtributoUseCase updateAtributoUseCase, GetAllAtributosUseCase getAllAtributosUseCase) {
        this.createAtributoUseCase = createAtributoUseCase;
        this.getAtributoByIdUseCase = getAtributoByIdUseCase;
        this.updateAtributoUseCase = updateAtributoUseCase;
        this.getAllAtributosUseCase = getAllAtributosUseCase;
    }

    @GetMapping
    public ResponseEntity<Page<AtributoResponse>> getAllAtributos(
            @RequestParam(value = "searchTerm", defaultValue = "") String searchTerm,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        Page<AtributoDto> atributoPage = getAllAtributosUseCase.getAll(searchTerm, pageable);
        Page<AtributoResponse> responsePage = atributoPage.map(this::toResponse);
        return ResponseEntity.ok(responsePage);
    }

    @PostMapping
    public ResponseEntity<AtributoResponse> createAtributo(@RequestBody CreateAtributoRequest request) {
        CreateAtributoCommand command = new CreateAtributoCommand(request.nome(), request.descricao());
        AtributoDto atributoDto = createAtributoUseCase.createAtributo(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(atributoDto.id()).toUri();
        return ResponseEntity.created(location).body(toResponse(atributoDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtributoResponse> getAtributoById(@PathVariable UUID id) {
        GetAtributoByIdCommand command = new GetAtributoByIdCommand(new AtributoId(id));
        return getAtributoByIdUseCase.getAtributoById(command)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtributoResponse> updateAtributo(@PathVariable UUID id, @RequestBody UpdateAtributoRequest request) {
        UpdateAtributoCommand command = new UpdateAtributoCommand(new AtributoId(id), request.nome(), request.descricao());
        AtributoDto atributoDto = updateAtributoUseCase.updateAtributo(command);
        return ResponseEntity.ok(toResponse(atributoDto));
    }

    private AtributoResponse toResponse(AtributoDto dto) {
        return new AtributoResponse(dto.id(), dto.nome(), dto.descricao());
    }
}
