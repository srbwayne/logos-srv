package com.josecjuniors.logossrv.adapters.in.web.regrafatorxp.api;

import com.josecjuniors.logossrv.adapters.in.web.regrafatorxp.dto.request.CreateRegraFatorXPRequest;
import com.josecjuniors.logossrv.adapters.in.web.regrafatorxp.dto.request.UpdateRegraFatorXPRequest;
import com.josecjuniors.logossrv.adapters.in.web.regrafatorxp.dto.response.RegraFatorXPResponse;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regrafatorxp.application.dto.RegraFatorXPDto;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.CreateRegraFatorXPCommand;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.CreateRegraFatorXPUseCase;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.DeleteRegraFatorXPUseCase;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.UpdateRegraFatorXPCommand;
import com.josecjuniors.logossrv.core.regrafatorxp.application.port.in.UpdateRegraFatorXPUseCase;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/regras-distribuicao/{regraDistribuicaoId}/fatores-xp")
public class RegraFatorXPController {

    private final CreateRegraFatorXPUseCase createUseCase;
    private final UpdateRegraFatorXPUseCase updateUseCase;
    private final DeleteRegraFatorXPUseCase deleteUseCase;

    public RegraFatorXPController(CreateRegraFatorXPUseCase createUseCase, UpdateRegraFatorXPUseCase updateUseCase, DeleteRegraFatorXPUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    public ResponseEntity<RegraFatorXPResponse> create(
            @PathVariable UUID regraDistribuicaoId,
            @RequestBody CreateRegraFatorXPRequest request) {
        var command = new CreateRegraFatorXPCommand(
                new RegraDistribuicaoAtividadeId(regraDistribuicaoId),
                new FatorCalculoId(request.fatorCalculoId()),
                request.pesoMultiplicador(),
                request.pontoCorteMin(),
                request.pontoCorteMax()
        );
        RegraFatorXPDto dto = createUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(RegraFatorXPResponse.fromDto(dto));
    }

    @PutMapping("/{regraFatorXPId}")
    public ResponseEntity<RegraFatorXPResponse> update(
            @PathVariable UUID regraFatorXPId,
            @RequestBody UpdateRegraFatorXPRequest request) {
        var command = new UpdateRegraFatorXPCommand(
                new RegraFatorXPId(regraFatorXPId),
                new FatorCalculoId(request.fatorCalculoId()),
                request.pesoMultiplicador(),
                request.pontoCorteMin(),
                request.pontoCorteMax()
        );
        RegraFatorXPDto dto = updateUseCase.update(command);
        return ResponseEntity.ok(RegraFatorXPResponse.fromDto(dto));
    }

    @DeleteMapping("/{regraFatorXPId}")
    public ResponseEntity<Void> delete(@PathVariable UUID regraFatorXPId) {
        deleteUseCase.delete(new RegraFatorXPId(regraFatorXPId));
        return ResponseEntity.noContent().build();
    }
}
