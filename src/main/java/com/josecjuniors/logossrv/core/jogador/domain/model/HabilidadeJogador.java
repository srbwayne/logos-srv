package com.josecjuniors.logossrv.core.jogador.domain.model;

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
public class HabilidadeJogador extends AbstractDomainAggregate<HabilidadeJogadorId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    @Column(nullable = false)
    private Integer nivelAtual;

    protected HabilidadeJogador() {
        super();
    }

    public HabilidadeJogador(HabilidadeJogadorId id, Jogador jogador, Habilidade habilidade) {
        super(id);
        this.jogador = jogador;
        this.habilidade = habilidade;
        this.nivelAtual = 1; // Habilidades sempre começam no nível 1
    }

    public void evoluirNivel() {
        this.nivelAtual++;
    }

    // Getters
    public Jogador getJogador() { return jogador; }
    public Habilidade getHabilidade() { return habilidade; }
    public Integer getNivelAtual() { return nivelAtual; }
}
