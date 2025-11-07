package com.josecjuniors.logossrv.core.registroatividade.domain.model;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.enums.SituacaoRegistroAtividade;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_atividade")
public class RegistroAtividade extends AbstractDomainAggregate<RegistroAtividadeId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_config_id", nullable = false)
    private AtividadeConfig atividadeConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoRegistroAtividade situacao;

    private LocalDateTime dataHoraInicio;

    private LocalDateTime dataHoraFim;

    private Duration horaAcumulada;

    private Integer xpGanhoFinal;

    private Integer estresseGerado;

    protected RegistroAtividade() {
        super();
    }

    public RegistroAtividade(RegistroAtividadeId id, Jogador jogador, AtividadeConfig atividadeConfig) {
        super(id);
        this.jogador = jogador;
        this.atividadeConfig = atividadeConfig;
        this.situacao = SituacaoRegistroAtividade.INICIADA;
        this.dataHoraInicio = LocalDateTime.now();
        this.horaAcumulada = Duration.ZERO;
    }

    // Getters
    public Jogador getJogador() { return jogador; }
    public AtividadeConfig getAtividadeConfig() { return atividadeConfig; }
    public SituacaoRegistroAtividade getSituacao() { return situacao; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public Duration getHoraAcumulada() { return horaAcumulada; }
    public Integer getXpGanhoFinal() { return xpGanhoFinal; }
    public Integer getEstresseGerado() { return estresseGerado; }
}
