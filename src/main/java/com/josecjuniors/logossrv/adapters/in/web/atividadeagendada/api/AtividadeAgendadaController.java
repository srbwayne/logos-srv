package com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.api;

import com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.dto.request.CreateAtividadeAgendadaRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.dto.request.ReagendarAtividadeRequest;
import com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.dto.response.AtividadeAgendadaResponse;
import com.josecjuniors.logossrv.core.atividadeagendada.application.dto.AtividadeAgendadaDto;
import com.josecjuniors.logossrv.core.atividadeagendada.application.port.in.*;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendadaId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jogadores/{jogadorId}/atividades-agendadas")
public class AtividadeAgendadaController {

    private final CreateAtividadeAgendadaUseCase createUseCase;
    private final GetAtividadesAgendadasPorPeriodoUseCase getByPeriodoUseCase;
    private final ReagendarAtividadeUseCase reagendarUseCase;

    public AtividadeAgendadaController(CreateAtividadeAgendadaUseCase createUseCase, GetAtividadesAgendadasPorPeriodoUseCase getByPeriodoUseCase, ReagendarAtividadeUseCase reagendarUseCase) {
        this.createUseCase = createUseCase;
        this.getByPeriodoUseCase = getByPeriodoUseCase;
        this.reagendarUseCase = reagendarUseCase;
    }

    @PostMapping
    public ResponseEntity<AtividadeAgendadaResponse> create(
            @PathVariable UUID jogadorId,
            @RequestBody CreateAtividadeAgendadaRequest request) {
        var command = new CreateAtividadeAgendadaCommand(
                new JogadorId(jogadorId),
                new AtividadeConfigId(request.atividadeConfigId()),
                request.dataHoraInicio(),
                request.dataHoraFim()
        );
        AtividadeAgendadaDto dto = createUseCase.create(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(location).body(AtividadeAgendadaResponse.fromDto(dto));
    }

    @GetMapping
    public ResponseEntity<List<AtividadeAgendadaResponse>> getByPeriodo(
            @PathVariable UUID jogadorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        List<AtividadeAgendadaDto> dtos = getByPeriodoUseCase.getByPeriodo(new JogadorId(jogadorId), inicio, fim);
        List<AtividadeAgendadaResponse> response = dtos.stream()
                .map(AtividadeAgendadaResponse::fromDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{agendamentoId}")
    public ResponseEntity<AtividadeAgendadaResponse> reagendar(
            @PathVariable UUID jogadorId,
            @PathVariable UUID agendamentoId,
            @RequestBody ReagendarAtividadeRequest request) {
        var command = new ReagendarAtividadeCommand(
                new JogadorId(jogadorId),
                new AtividadeAgendadaId(agendamentoId),
                request.novaDataHoraInicio(),
                request.novaDataHoraFim()
        );
        AtividadeAgendadaDto dto = reagendarUseCase.reagendar(command);
        return ResponseEntity.ok(AtividadeAgendadaResponse.fromDto(dto));
    }
}
