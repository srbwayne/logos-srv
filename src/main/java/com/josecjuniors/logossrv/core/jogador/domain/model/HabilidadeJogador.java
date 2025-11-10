package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.common.domain.Nivelavel;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "habilidade_jogador")
public class HabilidadeJogador extends AbstractDomainAggregate<HabilidadeJogadorId> implements Nivelavel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    @Column(nullable = false)
    private Long xpTotal;

    @Column(nullable = false)
    private Integer nivelAtual;

    protected HabilidadeJogador() {
        super();
    }

    public HabilidadeJogador(HabilidadeJogadorId id, Jogador jogador, Habilidade habilidade) {
        super(id);
        this.jogador = jogador;
        this.habilidade = habilidade;
        this.xpTotal = 0L;
        this.nivelAtual = 1;
    }

    @Override
    public void adicionarExperiencia(Long xpGanha) {
        this.xpTotal += xpGanha;
    }

    // Getters
    public Jogador getJogador() { return jogador; }
    public Habilidade getHabilidade() { return habilidade; }
    @Override
    public Long getXpTotal() { return xpTotal; }
    @Override
    public Integer getNivelAtual() { return nivelAtual; }

    // Setters da Interface
    @Override
    public void setNivelAtual(Integer nivel) { this.nivelAtual = nivel; }
    @Override
    public void setXpTotal(Long xp) { this.xpTotal = xp; }
}
