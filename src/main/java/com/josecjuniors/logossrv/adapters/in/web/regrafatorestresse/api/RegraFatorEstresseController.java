package com.josecjuniors.logossrv.adapters.in.web.regrafatorestresse.api;

import com.josecjuniors.logossrv.adapters.in.web.regrafatorestresse.dto.request.CreateRegraFatorEstresseRequest;
import com.josecjuniors.logossrv.adapters.in.web.regrafatorestresse.dto.request.UpdateRegraFatorEstresseRequest;
import com.josecjuniors.logossrv.adapters.in.web.regrafatorestresse.dto.response.RegraFatorEstresseResponse;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.dto.RegraFatorEstresseDto;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.CreateRegraFatorEstresseCommand;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.CreateRegraFatorEstresseUseCase;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.DeleteRegraFatorEstresseUseCase;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.UpdateRegraFatorEstresseCommand;
import com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in.UpdateRegraFatorEstresseUseCase;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-estresse")
public class RegraFatorEstresseController {

    private final CreateRegraFatorEstresseUseCase createUseCase;
    private final UpdateRegraFatorEstresseUseCase updateUseCase;
    private final DeleteRegraFatorEstresseUseCase deleteUseCase;

    public RegraFatorEstresseController(CreateRegraFatorEstresseUseCase createUseCase, UpdateRegraFatorEstresseUseCase updateUseCase, DeleteRegraFatorEstresseUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    public ResponseEntity<RegraFatorEstresseResponse> create(
            @PathVariable UUID regraDistribuicaoId,
            @RequestBody CreateRegraFatorEstresseRequest request) {
        var command = new CreateRegraFatorEstresseCommand(
                new RegraDistribuicaoAtividadeId(regraDistribuicaoId),
                request.pesoMultiplicador(),
                request.pontoCorteMin(),
                request.pontoCorteMax(),
                request.tipo()
        );
        RegraFatorEstresseDto dto = createUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(RegraFatorEstresseResponse.fromDto(dto));
    }

    @PutMapping("/{regraFatorEstresseId}")
    public ResponseEntity<RegraFatorEstresseResponse> update(
            @PathVariable UUID regraFatorEstresseId,
            @RequestBody UpdateRegraFatorEstresseRequest request) {
        var command = new UpdateRegraFatorEstresseCommand(
                new RegraFatorEstresseId(regraFatorEstresseId),
                request.pesoMultiplicador(),
                request.pontoCorteMin(),
                request.pontoCorteMax(),
                request.tipo()
        );
        RegraFatorEstresseDto dto = updateUseCase.update(command);
        return ResponseEntity.ok(RegraFatorEstresseResponse.fromDto(dto));
    }

    @DeleteMapping("/{regraFatorEstresseId}")
    public ResponseEntity<Void> delete(@PathVariable UUID regraFatorEstresseId) {
        deleteUseCase.delete(new RegraFatorEstresseId(regraFatorEstresseId));
        return ResponseEntity.noContent().build();
    }
}
