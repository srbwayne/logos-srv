package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model;

import com.josecjuniors.logossrv.core.habilidadejogador.domain.model.HabilidadeJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

@Entity
@Table(name = "regra_distribuicao_habilidade")
public class RegraDistribuicaoHabilidade extends AbstractDomainAggregate<RegraDistribuicaoHabilidadeId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_jogador_id", referencedColumnName = "id", nullable = false)
    private HabilidadeJogador habilidadeJogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_jogador_id", referencedColumnName = "id", nullable = false)
    private AtributoJogador atributoJogador;

    @Column(nullable = false)
    private Double pesoDistribuicao; // Ex: 0.7 para 70%

    protected RegraDistribuicaoHabilidade() {
        super();
    }

    public RegraDistribuicaoHabilidade(RegraDistribuicaoHabilidadeId id, HabilidadeJogador habilidadeJogador, AtributoJogador atributoJogador, Double pesoDistribuicao) {
        super(id);
        this.habilidadeJogador = habilidadeJogador;
        this.atributoJogador = atributoJogador;
        this.pesoDistribuicao = pesoDistribuicao;
    }

    // Getters
    public HabilidadeJogador getHabilidadeJogador() { return habilidadeJogador; }
    public AtributoJogador getAtributoJogador() { return atributoJogador; }
    public Double getPesoDistribuicao() { return pesoDistribuicao; }
}
