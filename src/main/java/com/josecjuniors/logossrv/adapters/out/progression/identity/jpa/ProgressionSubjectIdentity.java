package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "progression_subject_identity")
public class ProgressionSubjectIdentity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String namespace;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    protected ProgressionSubjectIdentity() {
    }

    public ProgressionSubjectIdentity(UUID id, String namespace, String externalId, Jogador jogador) {
        this.id = id;
        this.namespace = namespace;
        this.externalId = externalId;
        this.jogador = jogador;
    }

    public Jogador getJogador() {
        return jogador;
    }
}
