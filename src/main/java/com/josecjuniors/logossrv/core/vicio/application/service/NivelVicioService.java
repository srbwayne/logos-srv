package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogador;
import org.springframework.stereotype.Service;

@Service
public class NivelVicioService {

    public void adicionarExperiencia(VicioJogador vicioJogador, long xpGanha) {
        if (xpGanha <= 0) {
            return;
        }

        long xpAtual = vicioJogador.getXpTotal() + xpGanha;
        long xpParaProximoNivel = calcularXpParaProximoNivel(vicioJogador.getNivelAtual());

        while (xpAtual >= xpParaProximoNivel) {
            xpAtual -= xpParaProximoNivel;
            vicioJogador.setNivelAtual(vicioJogador.getNivelAtual() + 1);
            xpParaProximoNivel = calcularXpParaProximoNivel(vicioJogador.getNivelAtual());
        }
        vicioJogador.setXpTotal(xpAtual);
    }

    private long calcularXpParaProximoNivel(int nivel) {
        // A progressão de nível de um vício pode ser mais rápida no início
        return (long) (Math.pow(nivel, 2) * 50) + 25;
    }
}
