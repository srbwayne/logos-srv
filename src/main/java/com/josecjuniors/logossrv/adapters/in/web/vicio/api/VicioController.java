package com.josecjuniors.logossrv.adapters.in.web.vicio.api;

import com.josecjuniors.logossrv.adapters.in.web.vicio.dto.request.CreateRegraVicioRequest;
import com.josecjuniors.logossrv.adapters.in.web.vicio.dto.request.CreateVicioRequest;
import com.josecjuniors.logossrv.adapters.in.web.vicio.dto.request.UpdateRegraVicioRequest;
import com.josecjuniors.logossrv.adapters.in.web.vicio.dto.request.UpdateVicioRequest;
import com.josecjuniors.logossrv.adapters.in.web.vicio.dto.response.RegraVicioResponse;
import com.josecjuniors.logossrv.adapters.in.web.vicio.dto.response.VicioResponse;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.vicio.application.command.BuscarRegravicioPorIdCommand;
import com.josecjuniors.logossrv.core.vicio.application.command.CreateRegraVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.command.CreateVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.command.DeletarRegravicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.command.UpdateRegraVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.command.UpdateVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.RegraVicioDto;
import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;
import com.josecjuniors.logossrv.core.vicio.application.port.in.CreateRegraVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.application.port.in.CreateVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.application.port.in.DeleteRegraVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.application.port.in.GetAllRegrasByVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.application.port.in.GetAllViciosUseCase;
import com.josecjuniors.logossrv.core.vicio.application.port.in.GetRegraVicioByIdUseCase;
import com.josecjuniors.logossrv.core.vicio.application.port.in.GetVicioByIdUseCase;
import com.josecjuniors.logossrv.core.vicio.application.port.in.UpdateRegraVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.application.port.in.UpdateVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicioId;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vicios")
public class VicioController {

    private final CreateVicioUseCase createVicioUseCase;
    private final GetAllViciosUseCase getAllViciosUseCase;
    private final GetVicioByIdUseCase getVicioByIdUseCase;
    private final UpdateVicioUseCase updateVicioUseCase;

    private final CreateRegraVicioUseCase createRegraVicioUseCase;
    private final GetAllRegrasByVicioUseCase getAllRegrasByVicioUseCase;
    private final GetRegraVicioByIdUseCase getRegraVicioByIdUseCase;
    private final UpdateRegraVicioUseCase updateRegraVicioUseCase;
    private final DeleteRegraVicioUseCase deleteRegraVicioUseCase;

    public VicioController(CreateVicioUseCase createVicioUseCase, GetAllViciosUseCase getAllViciosUseCase, GetVicioByIdUseCase getVicioByIdUseCase, UpdateVicioUseCase updateVicioUseCase, CreateRegraVicioUseCase createRegraVicioUseCase, GetAllRegrasByVicioUseCase getAllRegrasByVicioUseCase, GetRegraVicioByIdUseCase getRegraVicioByIdUseCase, UpdateRegraVicioUseCase updateRegraVicioUseCase, DeleteRegraVicioUseCase deleteRegraVicioUseCase) {
        this.createVicioUseCase = createVicioUseCase;
        this.getAllViciosUseCase = getAllViciosUseCase;
        this.getVicioByIdUseCase = getVicioByIdUseCase;
        this.updateVicioUseCase = updateVicioUseCase;
        this.createRegraVicioUseCase = createRegraVicioUseCase;
        this.getAllRegrasByVicioUseCase = getAllRegrasByVicioUseCase;
        this.getRegraVicioByIdUseCase = getRegraVicioByIdUseCase;
        this.updateRegraVicioUseCase = updateRegraVicioUseCase;
        this.deleteRegraVicioUseCase = deleteRegraVicioUseCase;
    }

    // --- Vicio CRUD ---
    @PostMapping
    public ResponseEntity<VicioResponse> create(@RequestBody CreateVicioRequest request) {
        var command = new CreateVicioCommand(request.nome(), request.descricao());
        VicioDto dto = createVicioUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(VicioResponse.fromDto(dto));
    }

    @GetMapping
    public ResponseEntity<Page<VicioResponse>> getAll(@RequestParam(value = "searchTerm", defaultValue = "") String searchTerm,
                                                      @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        Page<VicioDto> page = getAllViciosUseCase.getAll(searchTerm, pageable);
        return ResponseEntity.ok(page.map(VicioResponse::fromDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VicioResponse> getById(@PathVariable UUID id) {
        return getVicioByIdUseCase.findById(new VicioId(id))
                .map(VicioResponse::fromDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<VicioResponse> update(@PathVariable UUID id, @RequestBody UpdateVicioRequest request) {
        var command = new UpdateVicioCommand(new VicioId(id), request.nome(), request.descricao());
        VicioDto dto = updateVicioUseCase.update(command);
        return ResponseEntity.ok(VicioResponse.fromDto(dto));
    }

    // --- RegraVicio CRUD ---
    @PostMapping("/{vicioId}/regras")
    public ResponseEntity<RegraVicioResponse> createRegra(@PathVariable UUID vicioId, @RequestBody CreateRegraVicioRequest request) {
        var command = new CreateRegraVicioCommand(
                new VicioId(vicioId),
                request.impactoEstresse(),
                request.penalidadePontos(),
                request.duracaoHoras(),
                request.xpGanhoRecaida(),
                request.debuffId() != null ? new DebuffId(request.debuffId()) : null
        );
        RegraVicioDto dto = createRegraVicioUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(RegraVicioResponse.fromDto(dto));
    }

    @GetMapping("/{vicioId}/regras")
    public ResponseEntity<List<RegraVicioResponse>> getAllRegras(@PathVariable UUID vicioId) {
        List<RegraVicioDto> dtos = getAllRegrasByVicioUseCase.getAll(new VicioId(vicioId));
        return ResponseEntity.ok(dtos.stream().map(RegraVicioResponse::fromDto).collect(Collectors.toList()));
    }

    @GetMapping("/{vicioId}/regras/{regraId}")
    public ResponseEntity<RegraVicioResponse> getRegraById(@PathVariable UUID vicioId, @PathVariable UUID regraId) {
        return getRegraVicioByIdUseCase.findById(new BuscarRegravicioPorIdCommand(new VicioId(vicioId), new RegraVicioId(regraId)))
                .map(RegraVicioResponse::fromDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{vicioId}/regras/{regraId}")
    public ResponseEntity<RegraVicioResponse> updateRegra(@PathVariable UUID vicioId, @PathVariable UUID regraId, @RequestBody UpdateRegraVicioRequest request) {
        var command = new UpdateRegraVicioCommand(
                new VicioId(vicioId),
                new RegraVicioId(regraId),
                request.impactoEstresse(),
                request.penalidadePontos(),
                request.duracaoHoras(),
                request.xpGanhoRecaida(),
                request.debuffId() != null ? new DebuffId(request.debuffId()) : null
        );
        RegraVicioDto dto = updateRegraVicioUseCase.update(command);
        return ResponseEntity.ok(RegraVicioResponse.fromDto(dto));
    }

    @DeleteMapping("/{vicioId}/regras/{regraId}")
    public ResponseEntity<Void> deleteRegra(@PathVariable UUID vicioId, @PathVariable UUID regraId) {
        deleteRegraVicioUseCase.delete(new DeletarRegravicioCommand(new VicioId(vicioId), new RegraVicioId(regraId)));
        return ResponseEntity.noContent().build();
    }
}
