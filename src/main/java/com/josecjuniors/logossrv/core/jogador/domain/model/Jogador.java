package com.josecjuniors.logossrv.core.jogador.domain.model;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.util.domain.AbstractDomainAggregate;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
public class Jogador extends AbstractDomainAggregate<JogadorId> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false)
    private String nomeExibicao;

    @OneToMany(mappedBy = "jogador", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AtributoJogador> atributosJogador = new HashSet<>();

    // Construtor para JPA
    protected Jogador() {
        super();
    }

    public Jogador(JogadorId id, AppUser user, String nomeExibicao) {
        super(id);
        this.user = user;
        this.nomeExibicao = nomeExibicao;
    }

    public AppUser getUser() {
        return user;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public Set<AtributoJogador> getAtributosJogador() {
        return atributosJogador;
    }

    /**
     * Adiciona um novo atributo ao jogador, se ainda não existir.
     * @param atributo O atributo global a ser associado.
     * @return O objeto AtributoJogador criado ou existente.
     */
    public AtributoJogador adicionarAtributo(Atributo atributo) {
        Optional<AtributoJogador> existente = this.atributosJogador.stream()
                .filter(aj -> aj.getAtributo().equals(atributo))
                .findFirst();
        return existente.orElseGet(() -> new AtributoJogador(this, atributo));
    }
}
