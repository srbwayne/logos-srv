package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.common.domain.Nivelavel;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "atributo_jogador")
public class AtributoJogador extends AbstractDomainAggregate<AtributoJogadorId> implements Nivelavel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_id", nullable = false)
    private Atributo atributo;

    @Column(name = "xp_total", nullable = false)
    private Long xpTotal;

    @Column(name = "nivel_atual", nullable = false)
    private Integer nivelAtual;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_penalizador_id")
    private AtributoJogador atributoPenalizador;

    protected AtributoJogador() {
        super();
    }

    public AtributoJogador(AtributoJogadorId id, Jogador jogador, Atributo atributo) {
        super(id);
        this.jogador = jogador;
        this.atributo = atributo;
        this.xpTotal = 0L;
        this.nivelAtual = 1;
    }

    @Override
    public void adicionarExperiencia(Long xpGanha) {
        this.xpTotal += xpGanha;
    }

    // Getters
    public Jogador getJogador() {
        return jogador;
    }

    public Atributo getAtributo() {
        return atributo;
    }

    @Override
    public Long getXpTotal() {
        return xpTotal;
    }

    @Override
    public Integer getNivelAtual() {
        return nivelAtual;
    }

    // Setters da Interface
    @Override
    public void setNivelAtual(Integer nivel) {
        this.nivelAtual = nivel;
    }

    @Override
    public void setXpTotal(Long xp) {
        this.xpTotal = xp;
    }
}
