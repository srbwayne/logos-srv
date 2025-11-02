package com.josecjuniors.logossrv.core.habilidadejogador.domain.model;

import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

@Entity
@Table(name = "habilidade_jogador")
public class HabilidadeJogador extends AbstractDomainAggregate<HabilidadeJogadorId> {

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

    // Getters
    public Jogador getJogador() { return jogador; }
    public Habilidade getHabilidade() { return habilidade; }
    public Long getXpTotal() { return xpTotal; }
    public Integer getNivelAtual() { return nivelAtual; }
}
