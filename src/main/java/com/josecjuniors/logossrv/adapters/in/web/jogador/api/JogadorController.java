package com.josecjuniors.logossrv.adapters.in.web.jogador.api;

import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.request.UpdateApelidoRequest;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.request.UpdatePerfilRequest;
import com.josecjuniors.logossrv.adapters.in.web.jogador.dto.response.JogadorProfileResponse;
import com.josecjuniors.logossrv.core.jogador.application.dto.JogadorDto;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdateApelidoCommand;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdateApelidoUseCase;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdatePerfilJogadorCommand;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdatePerfilJogadorUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/jogadores")
public class JogadorController {

    private final UpdatePerfilJogadorUseCase updatePerfilJogadorUseCase;
    private final UpdateApelidoUseCase updateApelidoUseCase;

    public JogadorController(UpdatePerfilJogadorUseCase updatePerfilJogadorUseCase, UpdateApelidoUseCase updateApelidoUseCase) {
        this.updatePerfilJogadorUseCase = updatePerfilJogadorUseCase;
        this.updateApelidoUseCase = updateApelidoUseCase;
    }

    @PutMapping("/meu-perfil")
    public ResponseEntity<JogadorProfileResponse> updateMeuPerfil(@RequestBody UpdatePerfilRequest request, Principal principal) {
        String userEmail = principal.getName();

        UpdatePerfilJogadorCommand command = new UpdatePerfilJogadorCommand(
                userEmail,
                request.nomeCompleto(),
                request.dataNascimento(),
                request.cpf(),
                request.numeroTelefone(),
                request.sexo(),
                request.descricao(),
                request.enderecoPais(),
                request.enderecoEstado(),
                request.enderecoCidade(),
                request.enderecoDescricao(),
                request.enderecoComplemento(),
                request.enderecoCep()
        );

        JogadorDto jogadorDto = updatePerfilJogadorUseCase.updatePerfil(command);

        return ResponseEntity.ok(JogadorProfileResponse.fromDto(jogadorDto));
    }

    @PatchMapping("/meu-perfil/apelido")
    public ResponseEntity<JogadorProfileResponse> updateMeuApelido(@RequestBody UpdateApelidoRequest request, Principal principal) {
        String userEmail = principal.getName();
        UpdateApelidoCommand command = new UpdateApelidoCommand(userEmail, request.apelido());
        JogadorDto jogadorDto = updateApelidoUseCase.updateApelido(command);
        return ResponseEntity.ok(JogadorProfileResponse.fromDto(jogadorDto));
    }
}
