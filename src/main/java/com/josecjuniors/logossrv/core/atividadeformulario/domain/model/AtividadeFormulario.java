package com.josecjuniors.logossrv.core.atividadeformulario.domain.model;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJsonConverter;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;

@Entity
@Table(name = "atividade_formulario")
public class AtividadeFormulario extends AbstractDomainAggregate<AtividadeFormularioId> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_config_id", nullable = false, unique = true)
    private AtividadeConfig atividadeConfig;

    @Convert(converter = AtividadeFormularioJsonConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb", nullable = false)
    private AtividadeFormularioJson formularioJson;

    @Column(nullable = false)
    private Integer versao;

    @Column(nullable = false)
    private LocalDateTime dataGeracao;

    protected AtividadeFormulario() {
        super();
    }

    public AtividadeFormulario(AtividadeFormularioId id, AtividadeConfig atividadeConfig, AtividadeFormularioJson formularioJson) {
        super(id);
        this.atividadeConfig = atividadeConfig;
        this.formularioJson = formularioJson;
        this.versao = 1;
        this.dataGeracao = LocalDateTime.now();
    }

    public void atualizar(AtividadeFormularioJson formularioJson) {
        this.formularioJson = formularioJson;
        this.versao++;
        this.dataGeracao = LocalDateTime.now();
    }

    // Getters
    public AtividadeConfig getAtividadeConfig() { return atividadeConfig; }
    public AtividadeFormularioJson getFormularioJson() { return formularioJson; }
    public Integer getVersao() { return versao; }
    public LocalDateTime getDataGeracao() { return dataGeracao; }
}
