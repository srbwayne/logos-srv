package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "debuff_jogador")
public class DebuffJogador extends AbstractDomainAggregate<DebuffJogadorId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debuff_id", nullable = false)
    private Debuff debuff;

    @Column(nullable = false)
    private Integer potencia;

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    protected DebuffJogador() {
        super();
    }

    public DebuffJogador(DebuffJogadorId id, Jogador jogador, Debuff debuff, Integer potencia, LocalDateTime dataExpiracao) {
        super(id);
        this.jogador = jogador;
        this.debuff = debuff;
        this.potencia = potencia;
        this.dataExpiracao = dataExpiracao;
    }

    public void acumular(Integer novaPotencia, LocalDateTime novaDataExpiracao) {
        this.potencia += novaPotencia;
        this.dataExpiracao = novaDataExpiracao;
    }

    // Getters
    public Jogador getJogador() { return jogador; }
    public Debuff getDebuff() { return debuff; }
    public Integer getPotencia() { return potencia; }
    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
}
