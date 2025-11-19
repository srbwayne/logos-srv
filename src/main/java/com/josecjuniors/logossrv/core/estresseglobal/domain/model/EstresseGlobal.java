package com.josecjuniors.logossrv.core.estresseglobal.domain.model;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "estresse_global")
public class EstresseGlobal extends AbstractDomainAggregate<EstresseGlobalId> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false, unique = true)
    private Jogador jogador;

    @Column(nullable = false)
    private Integer pontuacaoAtual;

    protected EstresseGlobal() {
        super();
    }

    public EstresseGlobal(EstresseGlobalId id, Jogador jogador) {
        super(id);
        this.jogador = jogador;
        this.pontuacaoAtual = 0; // O estresse sempre começa em 0
    }

    // Getters
    public Jogador getJogador() { return jogador; }
    public Integer getPontuacaoAtual() { return pontuacaoAtual; }

    // Métodos de negócio para manipular o estresse
    public void adicionarEstresse(int valor) {
        this.pontuacaoAtual += valor;
        if (this.pontuacaoAtual < 0) {
            this.pontuacaoAtual = 0;
        }
    }

    public void reduzirEstresse(int valor) {
        this.pontuacaoAtual = Math.max(0, this.pontuacaoAtual - valor);
    }
}
