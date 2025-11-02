package com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

@Entity
@Table(name = "regra_distribuicao_atividade")
public class RegraDistribuicaoAtividade extends AbstractDomainAggregate<RegraDistribuicaoAtividadeId> {

    @ManyToOne
    @JoinColumn(name = "atividade_id", referencedColumnName = "id")
    private AtividadeConfig atividadeConfig;

    @ManyToOne
    @JoinColumn(name = "atributo_jogador_id", referencedColumnName = "id")
    private AtributoJogador atributoJogador;

    private Double pesoPercentual;

    protected RegraDistribuicaoAtividade() {
        super();
    }

    public RegraDistribuicaoAtividade(RegraDistribuicaoAtividadeId id, AtividadeConfig atividadeConfig, AtributoJogador atributoJogador, Double pesoPercentual) {
        super(id);
        this.atividadeConfig = atividadeConfig;
        this.atributoJogador = atributoJogador;
        this.pesoPercentual = pesoPercentual;
    }

    public AtividadeConfig getAtividadeConfig() { return atividadeConfig; }
    public AtributoJogador getAtributoJogador() { return atributoJogador; }
    public Double getPesoPercentual() { return pesoPercentual; }
}
