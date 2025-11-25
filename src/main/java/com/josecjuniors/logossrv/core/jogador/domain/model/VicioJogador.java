package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vicio_jogador")
public class VicioJogador extends AbstractDomainAggregate<VicioJogadorId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vicio_id", nullable = false)
    private Vicio vicio;

    @Column(nullable = false)
    private boolean estaAtivo;

    private LocalDateTime dataInicio;

    private Long xpTotal;

    private Integer nivelAtual;

    protected VicioJogador() {
        super();
    }

    public VicioJogador(VicioJogadorId id, Jogador jogador, Vicio vicio) {
        super(id);
        this.jogador = jogador;
        this.vicio = vicio;
        this.estaAtivo = true;
        this.dataInicio = LocalDateTime.now();
        this.nivelAtual = 1;
        this.xpTotal = 0L;
    }

    // Getters
    public Jogador getJogador() { return jogador; }
    public Vicio getVicio() { return vicio; }
    public boolean isEstaAtivo() { return estaAtivo; }
    public LocalDateTime getDataInicio() { return dataInicio; }
    public Long getXpTotal() { return xpTotal; }
    public Integer getNivelAtual() { return nivelAtual; }
}
