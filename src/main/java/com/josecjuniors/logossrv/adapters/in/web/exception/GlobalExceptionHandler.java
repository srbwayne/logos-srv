package com.josecjuniors.logossrv.adapters.in.web.exception;

import com.josecjuniors.logossrv.core.appuser.domain.exception.EmailJaCadastradoException;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoJaExisteException;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoNaoEncontradoException;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeJaExisteException;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeNaoEncontradaException;
import com.josecjuniors.logossrv.core.jogador.domain.exception.ApelidoJaEmUsoException;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ... outros handlers ...

    @ExceptionHandler(HabilidadeJaExisteException.class)
    public ResponseEntity<Map<String, String>> handleHabilidadeJaExiste(HabilidadeJaExisteException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(HabilidadeNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleHabilidadeNaoEncontrada(HabilidadeNaoEncontradaException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AtributoJaExisteException.class)
    public ResponseEntity<Map<String, String>> handleAtributoJaExiste(AtributoJaExisteException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AtributoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleAtributoNaoEncontrado(AtributoNaoEncontradoException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(JogadorNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleJogadorNaoEncontrado(JogadorNaoEncontradoException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ApelidoJaEmUsoException.class)
    public ResponseEntity<Map<String, String>> handleApelidoJaEmUso(ApelidoJaEmUsoException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<Map<String, String>> handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.FORBIDDEN);
    }
}
