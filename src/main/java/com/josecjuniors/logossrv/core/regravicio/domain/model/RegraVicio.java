package com.josecjuniors.logossrv.core.regravicio.domain.model;

import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.Entity;

@Entity
public class RegraVicio extends AbstractDomainAggregate<RegraVicioId> {

    private Integer impactoEstresse;

    private Integer penalidadePontos;

    private Integer duracaoHoras;

    // Construtor para JPA
    protected RegraVicio() {
        super();
    }

    public RegraVicio(RegraVicioId id, Integer impactoEstresse, Integer penalidadePontos, Integer duracaoHoras) {
        super(id);
        this.impactoEstresse = impactoEstresse;
        this.penalidadePontos = penalidadePontos;
        this.duracaoHoras = duracaoHoras;
    }


    public Integer getImpactoEstresse() {
        return impactoEstresse;
    }

    public void setImpactoEstresse(Integer impactoEstresse) {
        this.impactoEstresse = impactoEstresse;
    }

    public Integer getPenalidadePontos() {
        return penalidadePontos;
    }

    public void setPenalidadePontos(Integer penalidadePontos) {
        this.penalidadePontos = penalidadePontos;
    }

    public Integer getDuracaoHoras() {
        return duracaoHoras;
    }

    public void setDuracaoHoras(Integer duracaoHoras) {
        this.duracaoHoras = duracaoHoras;
    }
}
