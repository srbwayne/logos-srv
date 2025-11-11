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
    private Habilidade habilidade; // A habilidade que SERÁ desbloqueada

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRequisito tipoRequisito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_requisito_id") // Pode ser nulo
    private Atributo atributoRequisito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_requisito_id") // Pode ser nulo
    private Habilidade habilidadeRequisito;

    @Column(nullable = false)
    private Integer nivelMinimo;

    // Construtor protegido para JPA
    protected HabilidadeRequisito() {
        super();
    }

    // Construtor para requisito de Atributo
    public HabilidadeRequisito(HabilidadeRequisitoId id, Habilidade habilidade, Atributo atributoRequisito, Integer nivelMinimo) {
        super(id);
        this.habilidade = habilidade;
        this.tipoRequisito = TipoRequisito.ATRIBUTO;
        this.atributoRequisito = atributoRequisito;
        this.nivelMinimo = nivelMinimo;
    }

    // Construtor para requisito de Habilidade
    public HabilidadeRequisito(HabilidadeRequisitoId id, Habilidade habilidade, Habilidade habilidadeRequisito, Integer nivelMinimo) {
        super(id);
        this.habilidade = habilidade;
        this.tipoRequisito = TipoRequisito.HABILIDADE;
        this.habilidadeRequisito = habilidadeRequisito;
        this.nivelMinimo = nivelMinimo;
    }

    // Métodos de fábrica estáticos que agora usam os construtores corretos
    public static HabilidadeRequisito paraAtributo(HabilidadeRequisitoId id, Habilidade habilidade, Atributo atributoRequisito, Integer nivelMinimo) {
        return new HabilidadeRequisito(id, habilidade, atributoRequisito, nivelMinimo);
    }

    public static HabilidadeRequisito paraHabilidade(HabilidadeRequisitoId id, Habilidade habilidade, Habilidade habilidadeRequisito, Integer nivelMinimo) {
        return new HabilidadeRequisito(id, habilidade, habilidadeRequisito, nivelMinimo);
    }

    // Getters
    public Habilidade getHabilidade() { return habilidade; }
    public TipoRequisito getTipoRequisito() { return tipoRequisito; }
    public Atributo getAtributoRequisito() { return atributoRequisito; }
    public Habilidade getHabilidadeRequisito() { return habilidadeRequisito; }
    public Integer getNivelMinimo() { return nivelMinimo; }
}
