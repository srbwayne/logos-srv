package com.josecjuniors.logossrv.adapters.in.web.regradistribuicaoatividade.api;

import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaoatividade.dto.request.CreateRegraDistribuicaoRequest;
import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaoatividade.dto.request.UpdateRegraDistribuicaoRequest;
import com.josecjuniors.logossrv.adapters.in.web.regradistribuicaoatividade.dto.response.RegraDistribuicaoAtividadeResponse;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.CreateRegraDistribuicaoCommand;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.CreateRegraDistribuicaoUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.DeleteRegraDistribuicaoUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.GetAllRegrasByAtividadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.UpdateRegraDistribuicaoCommand;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.UpdateRegraDistribuicaoUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
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
@RequestMapping("/api/atividades-config/{atividadeId}/regras-distribuicao")
public class RegraDistribuicaoAtividadeController {

    private final CreateRegraDistribuicaoUseCase createUseCase;
    private final UpdateRegraDistribuicaoUseCase updateUseCase;
    private final DeleteRegraDistribuicaoUseCase deleteUseCase;
    private final GetAllRegrasByAtividadeUseCase getAllUseCase;

    public RegraDistribuicaoAtividadeController(CreateRegraDistribuicaoUseCase createUseCase, UpdateRegraDistribuicaoUseCase updateUseCase, DeleteRegraDistribuicaoUseCase deleteUseCase, GetAllRegrasByAtividadeUseCase getAllUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getAllUseCase = getAllUseCase;
    }

    @PostMapping
    public ResponseEntity<RegraDistribuicaoAtividadeResponse> create(
            @PathVariable UUID atividadeId,
            @RequestBody CreateRegraDistribuicaoRequest request) {
        var command = new CreateRegraDistribuicaoCommand(new AtividadeConfigId(atividadeId), new AtributoId(request.atributoId()), request.pesoPercentual());
        RegraDistribuicaoAtividadeDto dto = createUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(RegraDistribuicaoAtividadeResponse.fromDto(dto));
    }

    @GetMapping
    public ResponseEntity<List<RegraDistribuicaoAtividadeResponse>> getAllByAtividade(@PathVariable UUID atividadeId) {
        List<RegraDistribuicaoAtividadeDto> dtos = getAllUseCase.getAllByAtividade(new AtividadeConfigId(atividadeId));
        List<RegraDistribuicaoAtividadeResponse> response = dtos.stream()
                .map(RegraDistribuicaoAtividadeResponse::fromDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{regraId}")
    public ResponseEntity<RegraDistribuicaoAtividadeResponse> update(
            @PathVariable UUID atividadeId,
            @PathVariable UUID regraId,
            @RequestBody UpdateRegraDistribuicaoRequest request) {
        var command = new UpdateRegraDistribuicaoCommand(new RegraDistribuicaoAtividadeId(regraId), request.pesoPercentual());
        RegraDistribuicaoAtividadeDto dto = updateUseCase.update(command);
        return ResponseEntity.ok(RegraDistribuicaoAtividadeResponse.fromDto(dto));
    }

    @DeleteMapping("/{regraId}")
    public ResponseEntity<Void> delete(@PathVariable UUID atividadeId, @PathVariable UUID regraId) {
        deleteUseCase.delete(new RegraDistribuicaoAtividadeId(regraId));
        return ResponseEntity.noContent().build();
    }
}
