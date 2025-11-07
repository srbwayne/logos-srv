package com.josecjuniors.logossrv.core.registroatividadedetalhe.domain.model;

import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "registro_atividade_detalhe")
public class RegistroAtividadeDetalhe extends AbstractDomainAggregate<RegistroAtividadeDetalheId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_atividade_id", nullable = false)
    private RegistroAtividade registroAtividade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fator_calculo_id", nullable = false)
    private FatorCalculo fatorCalculo;

    @Column(nullable = false)
    private Double valorRegistrado;

    protected RegistroAtividadeDetalhe() {
        super();
    }

    public RegistroAtividadeDetalhe(RegistroAtividadeDetalheId id, RegistroAtividade registroAtividade, FatorCalculo fatorCalculo, Double valorRegistrado) {
        super(id);
        this.registroAtividade = registroAtividade;
        this.fatorCalculo = fatorCalculo;
        this.valorRegistrado = valorRegistrado;
    }

    // Getters
    public RegistroAtividade getRegistroAtividade() { return registroAtividade; }
    public FatorCalculo getFatorCalculo() { return fatorCalculo; }
    public Double getValorRegistrado() { return valorRegistrado; }
}
