package com.josecjuniors.logossrv.core.vicio.domain.model;

import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

@Entity
@Table(name = "regra_vicio")
public class RegraVicio extends AbstractDomainAggregate<RegraVicioId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vicio_id", nullable = false)
    private Vicio vicio;

    private Integer impactoEstresse;
    private Integer penalidadePontos;
    private Integer duracaoHoras;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debuff_id")
    private Debuff debuff;

    protected RegraVicio() {
        super();
    }

    public RegraVicio(RegraVicioId id, Vicio vicio, Integer impactoEstresse, Integer penalidadePontos, Integer duracaoHoras, Debuff debuff) {
        super(id);
        this.vicio = vicio;
        this.impactoEstresse = impactoEstresse;
        this.penalidadePontos = penalidadePontos;
        this.duracaoHoras = duracaoHoras;
        this.debuff = debuff;
    }

    public void atualizar(Integer impactoEstresse, Integer penalidadePontos, Integer duracaoHoras, Debuff debuff) {
        this.impactoEstresse = impactoEstresse;
        this.penalidadePontos = penalidadePontos;
        this.duracaoHoras = duracaoHoras;
        this.debuff = debuff;
    }

    // Getters
    public Vicio getVicio() { return vicio; }
    public Integer getImpactoEstresse() { return impactoEstresse; }
    public Integer getPenalidadePontos() { return penalidadePontos; }
    public Integer getDuracaoHoras() { return duracaoHoras; }
    public Debuff getDebuff() { return debuff; }
}
