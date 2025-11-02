package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

@Entity
@Table(name = "atributo_jogador")
public class AtributoJogador extends AbstractDomainAggregate<AtributoJogadorId> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_id", nullable = false)
    private Atributo atributo;

    @Column(name = "xp_total", nullable = false)
    private Long xpTotal;

    @Column(name = "nivel_atual", nullable = false)
    private Integer nivelAtual;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_penalizador_id") // Pode ser nulo
    private AtributoJogador atributoPenalizador;

    protected AtributoJogador() {
        super();
    }

    public AtributoJogador(Jogador jogador, Atributo atributo) {
        super(AtributoJogadorId.generate());
        this.jogador = jogador;
        this.atributo = atributo;
        this.xpTotal = 0L;
        this.nivelAtual = 1;
    }

    public void adicionarExperiencia(Long xpGanha) {
        this.xpTotal += xpGanha;
        // TODO: Implementar lógica para verificar se o nível deve aumentar
    }

    // Getters
    public Jogador getJogador() { return jogador; }
    public Atributo getAtributo() { return atributo; }
    public Long getXpTotal() { return xpTotal; }
    public Integer getNivelAtual() { return nivelAtual; }
    public AtributoJogador getAtributoPenalizador() { return atributoPenalizador; }
}
