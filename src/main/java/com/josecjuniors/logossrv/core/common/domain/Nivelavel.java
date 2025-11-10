package com.josecjuniors.logossrv.core.common.domain;

/**
 * Interface para entidades que possuem um sistema de nível e experiência.
 */
public interface Nivelavel {

    Integer getNivelAtual();
    void setNivelAtual(Integer nivel);

    Long getXpTotal();
    void setXpTotal(Long xp);

    void adicionarExperiencia(Long xpGanha);
}
