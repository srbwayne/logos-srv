package com.josecjuniors.logossrv.core.atividadeagendada.domain.model;

import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.enums.StatusAtividadeAgendada;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "atividade_agendada")
public class AtividadeAgendada extends AbstractDomainAggregate<AtividadeAgendadaId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_config_id", nullable = false)
    private AtividadeConfig atividadeConfig;

    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAtividadeAgendada status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_atividade_id")
    private RegistroAtividade registroAtividade;

    protected AtividadeAgendada() {
        super();
    }

    public AtividadeAgendada(AtividadeAgendadaId id, Jogador jogador, AtividadeConfig atividadeConfig, LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim) {
        super(id);
        this.jogador = jogador;
        this.atividadeConfig = atividadeConfig;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
        this.status = StatusAtividadeAgendada.PLANEJADA;
    }

    /**
     * Reagenda a atividade, atualizando suas datas e status.
     * @throws IllegalStateException se a atividade não estiver no estado PLANEJADA.
     */
    public void reagendar(LocalDateTime novaDataHoraInicio, LocalDateTime novaDataHoraFim) {
        if (this.status != StatusAtividadeAgendada.PLANEJADA) {
            throw new IllegalStateException("Apenas atividades no estado 'PLANEJADA' podem ser reagendadas. Status atual: " + this.status);
        }
        this.dataHoraInicio = novaDataHoraInicio;
        this.dataHoraFim = novaDataHoraFim;
        this.status = StatusAtividadeAgendada.REAGENDADA;
    }

    // Getters
    public Jogador getJogador() { return jogador; }
    public AtividadeConfig getAtividadeConfig() { return atividadeConfig; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public StatusAtividadeAgendada getStatus() { return status; }
    public RegistroAtividade getRegistroAtividade() { return registroAtividade; }
}
