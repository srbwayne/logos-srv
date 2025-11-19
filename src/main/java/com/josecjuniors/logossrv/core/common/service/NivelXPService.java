package com.josecjuniors.logossrv.core.common.service;

import com.josecjuniors.logossrv.core.common.domain.Nivelavel;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import org.springframework.stereotype.Service;

@Service
public class NivelXPService {

    public void adicionarExperiencia(Nivelavel nivelavel, long xpGanha) {
        if (xpGanha <= 0) {
            return;
        }

        Jogador jogador = nivelavel.getJogadorAssociado();
        double modificadorEstresse = calcularModificadorEstresse(jogador.getEstresseGlobal().getPontuacaoAtual());
        long xpLiquido = (long) (xpGanha * modificadorEstresse);

        if (xpLiquido <= 0) {
            return;
        }

        long xpAtual = nivelavel.getXpTotal() + xpLiquido;
        long xpParaProximoNivel = calcularXpParaProximoNivel(nivelavel.getNivelAtual());

        while (xpAtual >= xpParaProximoNivel) {
            xpAtual -= xpParaProximoNivel;
            nivelavel.setNivelAtual(nivelavel.getNivelAtual() + 1);
            jogador.adicionarPontoDeHabilidade();
            xpParaProximoNivel = calcularXpParaProximoNivel(nivelavel.getNivelAtual());
        }
        nivelavel.adicionarExperiencia(xpGanha);
    }

    private double calcularModificadorEstresse(int estresseGlobal) {
        if (estresseGlobal >= 90) {
            return 0.5; // -50% de XP
        } else if (estresseGlobal >= 60) {
            return 0.7; // -30% de XP
        } else if (estresseGlobal >= 30) {
            return 0.85; // -15% de XP
        }
        return 1.0; // Sem penalidade
    }

    public long calcularXpParaProximoNivel(int nivel) {
        return (long) (Math.pow(nivel, 2) * 100) + 50;
    }
}
