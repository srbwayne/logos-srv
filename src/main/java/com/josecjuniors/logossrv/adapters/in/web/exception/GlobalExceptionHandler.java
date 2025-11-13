package com.josecjuniors.logossrv.adapters.in.web.exception;

import com.josecjuniors.logossrv.core.appuser.domain.exception.EmailJaCadastradoException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigJaExisteException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoJaExisteException;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoNaoEncontradoException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoJaExisteException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoNaoEncontradoException;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeJaExisteException;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeNaoEncontradaException;
import com.josecjuniors.logossrv.core.jogador.domain.exception.ApelidoJaEmUsoException;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.exception.RegraDistribuicaoNaoEncontradaException;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.exception.RegraFatorXPNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegraFatorXPNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleRegraFatorXPNaoEncontrada(RegraFatorXPNaoEncontradaException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RegraDistribuicaoNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleRegraDistribuicaoNaoEncontrada(RegraDistribuicaoNaoEncontradaException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AtividadeConfigJaExisteException.class)
    public ResponseEntity<Map<String, String>> handleAtividadeConfigJaExiste(AtividadeConfigJaExisteException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AtividadeConfigNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleAtividadeConfigNaoEncontrada(AtividadeConfigNaoEncontradaException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FatorCalculoJaExisteException.class)
    public ResponseEntity<Map<String, String>> handleFatorCalculoJaExiste(FatorCalculoJaExisteException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FatorCalculoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleFatorCalculoNaoEncontrado(FatorCalculoNaoEncontradoException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

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
