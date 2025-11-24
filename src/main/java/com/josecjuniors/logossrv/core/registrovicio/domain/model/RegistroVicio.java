package com.josecjuniors.logossrv.core.registrovicio.domain.model;

import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogador;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_vicio")
public class RegistroVicio extends AbstractDomainAggregate<RegistroVicioId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vicio_jogador_id", nullable = false)
    private VicioJogador vicioJogador;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Lob
    private String observacao;

    protected RegistroVicio() {
        super();
    }

    public RegistroVicio(RegistroVicioId id, VicioJogador vicioJogador, LocalDateTime dataHora, String observacao) {
        super(id);
        this.vicioJogador = vicioJogador;
        this.dataHora = dataHora;
        this.observacao = observacao;
    }

    // Getters
    public VicioJogador getVicioJogador() { return vicioJogador; }
    public LocalDateTime getDataHora() { return dataHora; }
    public String getObservacao() { return observacao; }
}
