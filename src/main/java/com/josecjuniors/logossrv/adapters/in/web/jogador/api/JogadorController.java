package com.josecjuniors.logossrv.adapters.in.web.jogador.api;

import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.request.UpdateApelidoRequest;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.request.UpdatePerfilRequest;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.response.AtributoJogadorResponse;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.response.JogadorProfileResponse;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.response.ResumoJogadorResponse;
import com.josecjuniors.logossrv.core.jogador.application.dto.AtributoJogadorDto;
import com.josecjuniors.logossrv.core.jogador.application.dto.JogadorDto;
import com.josecjuniors.logossrv.core.jogador.application.dto.ResumoJogadorDto;
import com.josecjuniors.logossrv.core.jogador.application.port.in.GetAtributosDoJogadorUseCase;
import com.josecjuniors.logossrv.core.jogador.application.port.in.GetResumoJogadorUseCase;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdateApelidoCommand;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdateApelidoUseCase;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdatePerfilJogadorCommand;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdatePerfilJogadorUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/jogadores")
public class JogadorController {

    private final UpdatePerfilJogadorUseCase updatePerfilJogadorUseCase;
    private final UpdateApelidoUseCase updateApelidoUseCase;
    private final GetAtributosDoJogadorUseCase getAtributosDoJogadorUseCase;
    private final GetResumoJogadorUseCase getResumoJogadorUseCase;

    public JogadorController(UpdatePerfilJogadorUseCase updatePerfilJogadorUseCase, UpdateApelidoUseCase updateApelidoUseCase, GetAtributosDoJogadorUseCase getAtributosDoJogadorUseCase, GetResumoJogadorUseCase getResumoJogadorUseCase) {
        this.updatePerfilJogadorUseCase = updatePerfilJogadorUseCase;
        this.updateApelidoUseCase = updateApelidoUseCase;
        this.getAtributosDoJogadorUseCase = getAtributosDoJogadorUseCase;
        this.getResumoJogadorUseCase = getResumoJogadorUseCase;
    }

    @GetMapping("/meu-resumo")
    public ResponseEntity<ResumoJogadorResponse> getMeuResumo(Principal principal) {
        ResumoJogadorDto resumoDto = getResumoJogadorUseCase.getResumo(principal.getName());
        return ResponseEntity.ok(ResumoJogadorResponse.fromDto(resumoDto));
    }

    @GetMapping("/meu-perfil/atributos")
    public ResponseEntity<Page<AtributoJogadorResponse>> getMeusAtributos(@PageableDefault(size = 10, sort = "atributo.nome") Pageable pageable, Principal principal) {
        Page<AtributoJogadorDto> pageDto = getAtributosDoJogadorUseCase.getAtributos(principal.getName(), pageable);
        Page<AtributoJogadorResponse> pageResponse = pageDto.map(AtributoJogadorResponse::fromDto);
        return ResponseEntity.ok(pageResponse);
    }

    @PutMapping("/meu-perfil")
    public ResponseEntity<JogadorProfileResponse> updateMeuPerfil(@RequestBody UpdatePerfilRequest request, Principal principal) {
        UpdatePerfilJogadorCommand command = new UpdatePerfilJogadorCommand(principal.getName(), request.nomeCompleto(), request.dataNascimento(), request.cpf(), request.numeroTelefone(), request.sexo(), request.descricao(), request.enderecoPais(), request.enderecoEstado(), request.enderecoCidade(), request.enderecoDescricao(), request.enderecoComplemento(), request.enderecoCep());
        JogadorDto jogadorDto = updatePerfilJogadorUseCase.updatePerfil(command);
        return ResponseEntity.ok(JogadorProfileResponse.fromDto(jogadorDto));
    }

    @PatchMapping("/meu-perfil/apelido")
    public ResponseEntity<JogadorProfileResponse> updateMeuApelido(@RequestBody UpdateApelidoRequest request, Principal principal) {
        UpdateApelidoCommand command = new UpdateApelidoCommand(principal.getName(), request.apelido());
        JogadorDto jogadorDto = updateApelidoUseCase.updateApelido(command);
        return ResponseEntity.ok(JogadorProfileResponse.fromDto(jogadorDto));
    }
}
