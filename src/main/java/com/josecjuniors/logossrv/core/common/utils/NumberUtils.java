package com.josecjuniors.logossrv.core.common.utils;

public class NumberUtils {
    public NumberUtils() {
    }

    public static boolean nuloOuMaiorOuIgualQue(Double limite, Double valor) {
        return limite == null || limite >= valor;
    }

    public static boolean nuloOuMenorOuIgualQue(Double limite, Double valor) {
        return limite == null || limite <= valor ;
    }

    public static boolean naoNuloEMaiorOuIgualQue(Double limite, Double valor) {
        return limite != null && limite >= valor;
    }

    public static boolean naoNuloEMenorOuIgualQue(Double limite, Double valor) {
        return limite != null && limite <= valor ;
    }
}
