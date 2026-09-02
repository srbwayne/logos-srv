package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.jogador.domain.model.HabilidadeJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidade;

/** Encapsula o bônus de habilidade destinado a um atributo. */
public class SkillBonusCalculator {

    public double calculate(Jogador jogador, Atributo atributo) {
        double bonusTotal = 0;
        for (HabilidadeJogador habilidadeJogador : jogador.getHabilidades()) {
            for (RegraDistribuicaoHabilidade regra : habilidadeJogador.getHabilidade().getRegrasDistribuicao()) {
                if (regra.getAtributo().equals(atributo)) {
                    bonusTotal += regra.getPesoDistribuicao() * habilidadeJogador.getNivelAtual() * 0.05;
                }
            }
        }
        return bonusTotal;
    }
}
