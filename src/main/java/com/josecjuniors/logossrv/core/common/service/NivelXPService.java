package com.josecjuniors.logossrv.core.common.service;

import com.josecjuniors.logossrv.core.common.domain.Nivelavel;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import org.springframework.stereotype.Service;

@Service
public class NivelXPService {

    private final ProgressionLevelCalculator levelCalculator = new ProgressionLevelCalculator();

    public void adicionarExperiencia(Nivelavel nivelavel, long xpGanha) {
        if (xpGanha <= 0) {
            return;
        }

        Jogador jogador = nivelavel.getJogadorAssociado();
        ProgressionLevelCalculator.LevelResult result = levelCalculator.apply(
                nivelavel.getNivelAtual(), nivelavel.getXpTotal(), jogador.getEstresseGlobal().getPontuacaoAtual(), xpGanha);
        if (!result.xpApplied()) {
            return;
        }
        nivelavel.setNivelAtual(result.level());
        for (int i = 0; i < result.levelsGained(); i++) {
            jogador.adicionarPontoDeHabilidade();
        }
        nivelavel.adicionarExperiencia(xpGanha);
    }

    public long calcularXpParaProximoNivel(int nivel) {
        return levelCalculator.xpForNextLevel(nivel);
    }
}
