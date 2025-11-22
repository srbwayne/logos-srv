package com.josecjuniors.logossrv.core.habilidade.domain.model;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.habilidade.domain.model.enums.TipoRequisito;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "habilidade_requisito")
public class HabilidadeRequisito extends AbstractDomainAggregate<HabilidadeRequisitoId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRequisito tipoRequisito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_requisito_id")
    private Atributo atributoRequisito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_requisito_id")
    private Habilidade habilidadeRequisito;

    @Column(nullable = false)
    private Integer nivelMinimo;

    protected HabilidadeRequisito() {
        super();
    }

    private HabilidadeRequisito(HabilidadeRequisitoId id, Habilidade habilidade, TipoRequisito tipo, Atributo atributo, Habilidade habilidadeReq, Integer nivel) {
        super(id);
        this.habilidade = habilidade;
        this.tipoRequisito = tipo;
        this.atributoRequisito = atributo;
        this.habilidadeRequisito = habilidadeReq;
        this.nivelMinimo = nivel;
    }

    public static HabilidadeRequisito paraAtributo(Habilidade habilidade, Atributo atributoRequisito, Integer nivelMinimo) {
        return new HabilidadeRequisito(HabilidadeRequisitoId.generate(), habilidade, TipoRequisito.ATRIBUTO, atributoRequisito, null, nivelMinimo);
    }

    public static HabilidadeRequisito paraHabilidade(Habilidade habilidade, Habilidade habilidadeRequisito, Integer nivelMinimo) {
        return new HabilidadeRequisito(HabilidadeRequisitoId.generate(), habilidade, TipoRequisito.HABILIDADE, null, habilidadeRequisito, nivelMinimo);
    }

    public static HabilidadeRequisito paraJogador(Habilidade habilidade, Integer nivelMinimo) {
        return new HabilidadeRequisito(HabilidadeRequisitoId.generate(), habilidade, TipoRequisito.JOGADOR, null, null, nivelMinimo);
    }
    
    // Getters
    public Habilidade getHabilidade() { return habilidade; }
    public TipoRequisito getTipoRequisito() { return tipoRequisito; }
    public Atributo getAtributoRequisito() { return atributoRequisito; }
    public Habilidade getHabilidadeRequisito() { return habilidadeRequisito; }
    public Integer getNivelMinimo() { return nivelMinimo; }
}
