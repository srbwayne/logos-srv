package com.josecjuniors.logossrv.adapters.in.web.auth.api;

import com.josecjuniors.logossrv.adapters.in.web.auth.dto.request.LoginRequest;
import com.josecjuniors.logossrv.adapters.in.web.auth.dto.request.RegistrationRequest;
import com.josecjuniors.logossrv.adapters.in.web.auth.dto.response.JwtResponse;
import com.josecjuniors.logossrv.adapters.in.web.auth.dto.response.RegistrationResponse;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.application.dto.RegistrationResult;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationUseCase registrationUseCase;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(RegistrationUseCase registrationUseCase, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.registrationUseCase = registrationUseCase;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationRequest request) {
        RegistrationCommand command = new RegistrationCommand(request.email(), request.password(), request.nomeExibicao());
        RegistrationResult result = registrationUseCase.register(command);

        RegistrationResponse response = new RegistrationResponse(
                result.user().getId().getValue().toString(),
                result.jogador().getId().getValue().toString(),
                result.user().getEmail(),
                result.jogador().getApelido()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AppUser user = (AppUser) authentication.getPrincipal();
        String jwt = jwtService.generateToken(user);

        return ResponseEntity.ok(new JwtResponse(jwt));
    }
}
