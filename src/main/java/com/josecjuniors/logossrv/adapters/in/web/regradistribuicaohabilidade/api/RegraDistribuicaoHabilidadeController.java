package com.josecjuniors.logossrv.adapters.in.web.regradistribuicaohabilidade.api;

import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaohabilidade.dto.request.CreateRegraDistribuicaoHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaohabilidade.dto.request.UpdateRegraDistribuicaoHabilidadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaohabilidade.dto.response.RegraDistribuicaoHabilidadeResponse;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.CreateRegraDistribuicaoHabilidadeCommand;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.DeletarRegradistribuicaoHabilidadeCommand;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.UpdateRegraDistribuicaoHabilidadeCommand;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.dto.RegraDistribuicaoHabilidadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in.CreateRegraDistribuicaoHabilidadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in.DeleteRegraDistribuicaoHabilidadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in.GetAllRegrasByHabilidadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in.UpdateRegraDistribuicaoHabilidadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidadeId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/habilidades/{habilidadeId}/regras-distribuicao")
public class RegraDistribuicaoHabilidadeController {

    private final CreateRegraDistribuicaoHabilidadeUseCase createUseCase;
    private final GetAllRegrasByHabilidadeUseCase getAllUseCase;
    private final UpdateRegraDistribuicaoHabilidadeUseCase updateUseCase;
    private final DeleteRegraDistribuicaoHabilidadeUseCase deleteUseCase;

    public RegraDistribuicaoHabilidadeController(CreateRegraDistribuicaoHabilidadeUseCase createUseCase, GetAllRegrasByHabilidadeUseCase getAllUseCase, UpdateRegraDistribuicaoHabilidadeUseCase updateUseCase, DeleteRegraDistribuicaoHabilidadeUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.getAllUseCase = getAllUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    public ResponseEntity<RegraDistribuicaoHabilidadeResponse> create(@PathVariable UUID habilidadeId, @RequestBody CreateRegraDistribuicaoHabilidadeRequest request) {
        var command = new CreateRegraDistribuicaoHabilidadeCommand(new HabilidadeId(habilidadeId), new AtributoId(request.atributoId()), request.pesoDistribuicao());
        RegraDistribuicaoHabilidadeDto dto = createUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(RegraDistribuicaoHabilidadeResponse.fromDto(dto));
    }

    @GetMapping
    public ResponseEntity<List<RegraDistribuicaoHabilidadeResponse>> getAll(@PathVariable UUID habilidadeId) {
        List<RegraDistribuicaoHabilidadeDto> dtos = getAllUseCase.getAllByHabilidade(new HabilidadeId(habilidadeId));
        List<RegraDistribuicaoHabilidadeResponse> response = dtos.stream()
                .map(RegraDistribuicaoHabilidadeResponse::fromDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{regraDistribuicaoHabilidadeId}")
    public ResponseEntity<RegraDistribuicaoHabilidadeResponse> update(@PathVariable UUID habilidadeId, @PathVariable UUID regraDistribuicaoHabilidadeId, @RequestBody UpdateRegraDistribuicaoHabilidadeRequest request) {
        var command = new UpdateRegraDistribuicaoHabilidadeCommand(new HabilidadeId(habilidadeId), new RegraDistribuicaoHabilidadeId(regraDistribuicaoHabilidadeId), request.pesoDistribuicao());
        RegraDistribuicaoHabilidadeDto dto = updateUseCase.update(command);
        return ResponseEntity.ok(RegraDistribuicaoHabilidadeResponse.fromDto(dto));
    }

    @DeleteMapping("/{regraDistribuicaoHabilidadeId}")
    public ResponseEntity<Void> delete(@PathVariable UUID habilidadeId, @PathVariable UUID regraDistribuicaoHabilidadeId) {
        deleteUseCase.delete(new DeletarRegradistribuicaoHabilidadeCommand(new HabilidadeId(habilidadeId), new RegraDistribuicaoHabilidadeId(regraDistribuicaoHabilidadeId)));
        return ResponseEntity.noContent().build();
    }
}
