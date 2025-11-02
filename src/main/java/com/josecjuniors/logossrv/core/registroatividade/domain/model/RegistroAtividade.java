package com.josecjuniors.logossrv.core.registroatividade.domain.model;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RegistroAtividade extends AbstractDomainAggregate<RegistroAtividadeId> {

    @ManyToOne
    @JoinColumn(name = "jogador_id", referencedColumnName = "id")
    private Jogador jogador;

    @ManyToOne
    @JoinColumn(name = "atividade_config_id", referencedColumnName = "id")
    private AtividadeConfig atividadeConfig;

    private LocalDateTime dataHora;

    private Integer xpGanhoFinal;

    private Integer estresseGerado;

    // Construtor para JPA
    protected RegistroAtividade() {
        super();
    }

    public RegistroAtividade(RegistroAtividadeId id, Jogador jogador, AtividadeConfig atividadeConfig, LocalDateTime dataHora, Integer xpGanhoFinal, Integer estresseGerado) {
        super(id);
        this.jogador = jogador;
        this.atividadeConfig = atividadeConfig;
        this.dataHora = dataHora;
        this.xpGanhoFinal = xpGanhoFinal;
        this.estresseGerado = estresseGerado;
    }

    public Jogador getJogador() {
        return jogador;
    }

    public void setJogador(Jogador jogador) {
        this.jogador = jogador;
    }

    public AtividadeConfig getAtividadeConfig() {
        return atividadeConfig;
    }

    public void setAtividadeConfig(AtividadeConfig atividadeConfig) {
        this.atividadeConfig = atividadeConfig;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Integer getXpGanhoFinal() {
        return xpGanhoFinal;
    }

    public void setXpGanhoFinal(Integer xpGanhoFinal) {
        this.xpGanhoFinal = xpGanhoFinal;
    }

    public Integer getEstresseGerado() {
        return estresseGerado;
    }

    public void setEstresseGerado(Integer estresseGerado) {
        this.estresseGerado = estresseGerado;
    }
}
