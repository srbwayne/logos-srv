package com.josecjuniors.logossrv.core.vicio.domain.exception;

public class RegraVicioNaoPertenceAoVicioException extends SecurityException {

    public  RegraVicioNaoPertenceAoVicioException() {
        super("Esta regra de Vicio não pertence ao Vicio informado.");
    }
}
