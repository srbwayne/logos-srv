package com.josecjuniors.logossrv.core.registroatividade.domain.model;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.enums.SituacaoRegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.enums.StatusProcessamento;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "registro_atividade")
public class RegistroAtividade extends AbstractDomainAggregate<RegistroAtividadeId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_config_id", nullable = false)
    private AtividadeConfig atividadeConfig;

    @Column(nullable = false)
    private LocalDateTime dataRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_processamento", nullable = false) // Nome da coluna explícito
    private StatusProcessamento statusProcessamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoRegistroAtividade situacao;

    private LocalDateTime dataHoraInicio;

    private LocalDateTime dataHoraFim;

    private Duration horaAcumulada;

    private Integer xpGanhoFinal;

    private Integer estresseGerado;

    @Column(name = "configuration_version_id")
    private UUID configurationVersionId;

    @Column(name = "skill_policy_version_id")
    private UUID skillPolicyVersionId;

    @OneToMany(mappedBy = "registroAtividade", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroAtividadeDetalhe> detalhes = new ArrayList<>();

    protected RegistroAtividade() {
        super();
    }

    public RegistroAtividade(RegistroAtividadeId id, Jogador jogador, AtividadeConfig atividadeConfig, LocalDateTime dataHoraInicio,
                             LocalDateTime dataHoraFim) {
        super(id);
        this.jogador = jogador;
        this.atividadeConfig = atividadeConfig;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
        this.dataRegistro = LocalDateTime.now();
        this.statusProcessamento = StatusProcessamento.PENDENTE;
        this.situacao = SituacaoRegistroAtividade.CONCLUIDA;
    }

    public void adicionarDetalhe(FatorCalculo fator, String valor) {
        RegistroAtividadeDetalhe detalhe = new RegistroAtividadeDetalhe(
                RegistroAtividadeDetalheId.generate(),
                this,
                fator,
                valor
        );
        this.detalhes.add(detalhe);
    }

    public void marcarComoProcessado(int xpFinal, int estresseFinal) {
        marcarComoProcessado(xpFinal, estresseFinal, null, null);
    }

    public void marcarComoProcessado(int xpFinal, int estresseFinal, UUID configurationVersionId,
                                     UUID skillPolicyVersionId) {
        this.xpGanhoFinal = xpFinal;
        this.estresseGerado = estresseFinal;
        this.configurationVersionId = configurationVersionId;
        this.skillPolicyVersionId = skillPolicyVersionId;
        this.statusProcessamento = StatusProcessamento.PROCESSADO;
    }

    // Getters
    public Jogador getJogador() {
        return jogador;
    }

    public AtividadeConfig getAtividadeConfig() {
        return atividadeConfig;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public StatusProcessamento getStatusProcessamento() {
        return statusProcessamento;
    }

    public List<RegistroAtividadeDetalhe> getDetalhes() {
        return detalhes;
    }

    public LocalDateTime getDataHoraInicio() {
        return dataHoraInicio;
    }

    public LocalDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public Duration getHoraAcumulada() {
        return horaAcumulada;
    }

    public Integer getXpGanhoFinal() {
        return xpGanhoFinal;
    }

    public Integer getEstresseGerado() {
        return estresseGerado;
    }

    public UUID getConfigurationVersionId() { return configurationVersionId; }

    public UUID getSkillPolicyVersionId() { return skillPolicyVersionId; }

    public SituacaoRegistroAtividade getSituacao() {
        return situacao;
    }
}
