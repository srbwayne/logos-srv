package com.josecjuniors.logossrv.core.common.service;

import com.josecjuniors.logossrv.core.common.domain.Nivelavel;
import org.springframework.stereotype.Service;

@Service
public class NivelXPService {

    private static final int CURVA_BASE_XP = 100;
    private static final int CONSTANTE_BASE_XP = 50;

    /**
     * Calcula o total de XP necessário para alcançar o próximo nível.
     * Fórmula exponencial: (nivelAtual * nivelAtual * CURVA_BASE_XP) + CONSTANTE_BASE_XP
     * @param nivelAtual O nível atual da entidade.
     * @return O total de XP para o próximo nível.
     */
    public Long getXpParaProximoNivel(int nivelAtual) {
        return (long) (nivelAtual * nivelAtual * CURVA_BASE_XP) + CONSTANTE_BASE_XP;
    }

    /**
     * Adiciona experiência a uma entidade Nivelavel e atualiza seu nível se necessário.
     * @param entidade A entidade que receberá o XP (ex: AtributoJogador, HabilidadeJogador).
     * @param xpGanha A quantidade de XP a ser adicionada.
     */
    public void adicionarExperiencia(Nivelavel entidade, Long xpGanha) {
        if (xpGanha <= 0) {
            return;
        }

        entidade.adicionarExperiencia(xpGanha);

        Long xpNecessario = getXpParaProximoNivel(entidade.getNivelAtual());

        // Loop para permitir múltiplos "level ups" com uma única grande quantidade de XP
        while (entidade.getXpTotal() >= xpNecessario) {
            // Sobe de nível
            entidade.setNivelAtual(entidade.getNivelAtual() + 1);
            
            // O XP excedente é mantido para o próximo nível (opcional, mas comum em RPGs)
            // Se não quiséssemos manter, faríamos: entidade.setXpTotal(entidade.getXpTotal() - xpNecessario);
            
            // Recalcula o XP necessário para o novo nível
            xpNecessario = getXpParaProximoNivel(entidade.getNivelAtual());
        }
    }
}
