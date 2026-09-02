package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeDetalhe;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Compõe os cálculos de progressão sem acessar persistência, eventos ou infraestrutura. */
@Service
public class ProgressionEngine {

    private final XpCalculator xpCalculator;
    private final StressCalculator stressCalculator;
    private final SkillBonusCalculator skillBonusCalculator;

    public ProgressionEngine() {
        this(new XpCalculator(), new StressCalculator(), new SkillBonusCalculator());
    }

    ProgressionEngine(XpCalculator xpCalculator, StressCalculator stressCalculator, SkillBonusCalculator skillBonusCalculator) {
        this.xpCalculator = xpCalculator;
        this.stressCalculator = stressCalculator;
        this.skillBonusCalculator = skillBonusCalculator;
    }

    public ProgressionResult calculate(RegistroAtividade registro) {
        int xpBase = registro.getAtividadeConfig().getXpBase();
        int estresseBase = registro.getAtividadeConfig().getEstresseBase();
        Map<RegraDistribuicaoAtividade, Double> xpPorRegra = new HashMap<>();
        double estresseAcumulado = estresseBase;

        var detalhesCalculaveis = registro.getDetalhes().stream()
                .filter(detalhe -> detalhe.getFatorCalculo().getTipoInput().ehValorNumerico())
                .toList();
        for (RegistroAtividadeDetalhe detalhe : detalhesCalculaveis) {
            double valorDetalhe = Double.parseDouble(detalhe.getValorRegistrado());
            for (RegraDistribuicaoAtividade regraDist : registro.getAtividadeConfig().getRegrasDistribuicao()) {
                for (RegraFatorXP regraXP : regraDist.getRegraFatorXPS()) {
                    if (regraXP.getFatorCalculo().equals(detalhe.getFatorCalculo())) {
                        double xpCalculado = xpCalculator.calculate(regraXP, valorDetalhe, xpBase, regraDist.getPesoPercentual());
                        xpPorRegra.merge(regraDist, xpCalculado, Double::sum);
                    }
                }
                for (RegraFatorEstresse regraEstresse : regraDist.getRegraFatorEstresses()) {
                    estresseAcumulado += stressCalculator.calculate(regraEstresse, valorDetalhe, estresseBase);
                }
            }
        }

        long xpTotal = xpPorRegra.isEmpty() ? xpBase : xpPorRegra.values().stream().mapToLong(Double::longValue).sum();
        Jogador jogador = registro.getJogador();
        List<ProgressionResult.AttributeProgression> attributeProgressions = new ArrayList<>();
        xpPorRegra.forEach((regra, xp) -> {
            AtributoJogador atributoJogador = jogador.getAtributo(regra.getAtributo());
            double bonusXP = skillBonusCalculator.calculate(jogador, regra.getAtributo());
            attributeProgressions.add(new ProgressionResult.AttributeProgression(
                    atributoJogador == null ? regra.getAtributo() : atributoJogador.getAtributo(),
                    xp.longValue() + (long) bonusXP));
        });
        return new ProgressionResult(xpTotal, estresseAcumulado, attributeProgressions);
    }
}
