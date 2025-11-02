package com.josecjuniors.logossrv.core.registrovicio.domain.model;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.regravicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_vicio")
public class RegistroVicio extends AbstractDomainAggregate<RegistroVicioId> {

    @ManyToOne
    @JoinColumn(name = "jogador_id", referencedColumnName = "id")
    private Jogador jogador;

    @ManyToOne
    @JoinColumn(name = "regra_vicio_id", referencedColumnName = "id")
    private RegraVicio regraVicio;

    private LocalDateTime dataHora;

    private Boolean estaAtivo;

    protected RegistroVicio() {
        super();
    }

    public RegistroVicio(RegistroVicioId id, Jogador jogador, RegraVicio regraVicio, LocalDateTime dataHora, Boolean estaAtivo) {
        super(id);
        this.jogador = jogador;
        this.regraVicio = regraVicio;
        this.dataHora = dataHora;
        this.estaAtivo = estaAtivo;
    }

    public Jogador getJogador() { return jogador; }
    public RegraVicio getRegraVicio() { return regraVicio; }
    public LocalDateTime getDataHora() { return dataHora; }
    public Boolean getEstaAtivo() { return estaAtivo; }
}
