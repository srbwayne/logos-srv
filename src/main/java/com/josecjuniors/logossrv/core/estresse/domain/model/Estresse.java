package com.josecjuniors.logossrv.core.estresse.domain.model;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class Estresse extends AbstractDomainAggregate<EstresseId> {

    @OneToOne
    @JoinColumn(name = "jogador_id", referencedColumnName = "id")
    private Jogador jogador;

    private Integer pontuacaoAtual;

    // Construtor para JPA
    protected Estresse() {
        super();
    }

    public Estresse(EstresseId id, Jogador jogador, Integer pontuacaoAtual) {
        super(id);
        this.jogador = jogador;
        this.pontuacaoAtual = pontuacaoAtual;
    }

    public Jogador getJogador() {
        return jogador;
    }

    public void setJogador(Jogador jogador) {
        this.jogador = jogador;
    }

    public Integer getPontuacaoAtual() {
        return pontuacaoAtual;
    }

    public void setPontuacaoAtual(Integer pontuacaoAtual) {
        this.pontuacaoAtual = pontuacaoAtual;
    }
}
