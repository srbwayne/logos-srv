package com.josecjuniors.logossrv.adapters.in.web.debuff.api;

import com.josecjuniors.logossrv.adapters.in.web.debuff.dto.request.CreateDebuffRequest;
import com.josecjuniors.logossrv.adapters.in.web.debuff.dto.request.CreateRegraDistribuicaoDebuffRequest;
import com.josecjuniors.logossrv.adapters.in.web.debuff.dto.request.UpdateDebuffRequest;
import com.josecjuniors.logossrv.adapters.in.web.debuff.dto.response.DebuffResponse;
import com.josecjuniors.logossrv.adapters.in.web.debuff.dto.response.RegraDistribuicaoDebuffResponse;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.debuff.application.command.BuscarRegraDistribuicaoDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.command.CreateDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.command.CreateRegraDistribuicaoDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.command.DeleteRegraDistribuicaoDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.command.UpdateDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;
import com.josecjuniors.logossrv.core.debuff.application.dto.RegraDistribuicaoDebuffDto;
import com.josecjuniors.logossrv.core.debuff.application.port.in.*;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuffId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/debuffs")
public class DebuffController {

    private final CreateDebuffUseCase createDebuffUseCase;
    private final GetAllDebuffsUseCase getAllDebuffsUseCase;
    private final GetDebuffByIdUseCase getDebuffByIdUseCase;
    private final UpdateDebuffUseCase updateDebuffUseCase;
    private final CreateRegraDistribuicaoDebuffUseCase createRegraUseCase;
    private final GetAllRegrasByDebuffUseCase getAllRegrasUseCase;
    private final GetRegraDistribuicaoDebuffByIdUseCase getRegraByIdUseCase;
    private final DeleteRegraDistribuicaoDebuffUseCase deleteRegraUseCase;

    public DebuffController(CreateDebuffUseCase createDebuffUseCase, GetAllDebuffsUseCase getAllDebuffsUseCase, GetDebuffByIdUseCase getDebuffByIdUseCase, UpdateDebuffUseCase updateDebuffUseCase, CreateRegraDistribuicaoDebuffUseCase createRegraUseCase, GetAllRegrasByDebuffUseCase getAllRegrasUseCase, GetRegraDistribuicaoDebuffByIdUseCase getRegraByIdUseCase, DeleteRegraDistribuicaoDebuffUseCase deleteRegraUseCase) {
        this.createDebuffUseCase = createDebuffUseCase;
        this.getAllDebuffsUseCase = getAllDebuffsUseCase;
        this.getDebuffByIdUseCase = getDebuffByIdUseCase;
        this.updateDebuffUseCase = updateDebuffUseCase;
        this.createRegraUseCase = createRegraUseCase;
        this.getAllRegrasUseCase = getAllRegrasUseCase;
        this.getRegraByIdUseCase = getRegraByIdUseCase;
        this.deleteRegraUseCase = deleteRegraUseCase;
    }

    // CRUD Debuff
    @PostMapping
    public ResponseEntity<DebuffResponse> create(@RequestBody CreateDebuffRequest request) {
        var command = new CreateDebuffCommand(request.nome());
        DebuffDto dto = createDebuffUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(DebuffResponse.fromDto(dto));
    }

    @GetMapping
    public ResponseEntity<Page<DebuffResponse>> getAll(@RequestParam(value = "searchTerm", defaultValue = "") String searchTerm,
                                                       @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        Page<DebuffDto> debuffPage = getAllDebuffsUseCase.getAll(searchTerm, pageable);
        return ResponseEntity.ok(debuffPage.map(DebuffResponse::fromDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DebuffResponse> getById(@PathVariable UUID id) {
        return getDebuffByIdUseCase.findById(new DebuffId(id))
                .map(DebuffResponse::fromDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DebuffResponse> update(@PathVariable UUID id, @RequestBody UpdateDebuffRequest request) {
        var command = new UpdateDebuffCommand(new DebuffId(id), request.nome());
        DebuffDto dto = updateDebuffUseCase.update(command);
        return ResponseEntity.ok(DebuffResponse.fromDto(dto));
    }

    // CRUD RegraDistribuicaoDebuff
    @PostMapping("/{debuffId}/regras-distribuicao")
    public ResponseEntity<RegraDistribuicaoDebuffResponse> createRegra(@PathVariable UUID debuffId, @RequestBody CreateRegraDistribuicaoDebuffRequest request) {
        var command = new CreateRegraDistribuicaoDebuffCommand(new DebuffId(debuffId), new AtributoId(request.atributoId()));
        RegraDistribuicaoDebuffDto dto = createRegraUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(RegraDistribuicaoDebuffResponse.fromDto(dto));
    }

    @GetMapping("/{debuffId}/regras-distribuicao")
    public ResponseEntity<Page<RegraDistribuicaoDebuffResponse>> getAllRegras(
            @PathVariable UUID debuffId,
            @RequestParam(value = "searchTerm", defaultValue = "") String searchTerm,
            @PageableDefault(size = 10, sort = "atributo.nome") Pageable pageable) {
        Page<RegraDistribuicaoDebuffDto> page = getAllRegrasUseCase.getAll(new DebuffId(debuffId), searchTerm, pageable);
        return ResponseEntity.ok(page.map(RegraDistribuicaoDebuffResponse::fromDto));
    }

    @GetMapping("/{debuffId}/regras-distribuicao/{regraId}")
    public ResponseEntity<RegraDistribuicaoDebuffResponse> getRegraById(@PathVariable UUID debuffId, @PathVariable UUID regraId) {
        return getRegraByIdUseCase.findById(new BuscarRegraDistribuicaoDebuffCommand(new DebuffId(debuffId), new RegraDistribuicaoDebuffId(regraId)))
                .map(RegraDistribuicaoDebuffResponse::fromDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{debuffId}/regras-distribuicao/{regraId}")
    public ResponseEntity<Void> deleteRegra(@PathVariable UUID debuffId, @PathVariable UUID regraId) {
        var command = new DeleteRegraDistribuicaoDebuffCommand(new DebuffId(debuffId), new RegraDistribuicaoDebuffId(regraId));
        deleteRegraUseCase.delete(command);
        return ResponseEntity.noContent().build();
    }
}
