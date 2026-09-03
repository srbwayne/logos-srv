package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;

import java.util.Collection;
import java.util.List;

/** Adapta o agregado persistente atual para o estado de progressão e de volta. */
public class ProgressionProfileMapper {

    public ProgressionProfile from(Jogador jogador) {
        List<ProgressionProfile.ProgressionAttribute> attributes = jogador.getAtributos().stream()
                .map(attribute -> new ProgressionProfile.ProgressionAttribute(
                        key(attribute.getAtributo()), attribute.getXpTotal(), attribute.getNivelAtual()))
                .toList();
        List<ProgressionProfile.SkillState> skills = jogador.getHabilidades().stream()
                .map(skill -> new ProgressionProfile.SkillState(key(skill.getHabilidade()), skill.getNivelAtual()))
                .toList();
        return new ProgressionProfile(
                jogador.getXpTotal(), jogador.getNivelAtual(), jogador.getEstresseGlobal().getPontuacaoAtual(),
                jogador.getPontosHabilidade(), attributes, skills);
    }

    public void applyTo(Jogador jogador, ProgressionProfile profile, Collection<Atributo> availableAttributes) {
        jogador.setXpTotal(profile.globalXp());
        jogador.setNivelAtual(profile.globalLevel());
        jogador.aplicarEstresse(profile.stress() - jogador.getEstresseGlobal().getPontuacaoAtual());

        int pointsToAdd = profile.skillPoints() - jogador.getPontosHabilidade();
        for (int i = 0; i < pointsToAdd; i++) {
            jogador.adicionarPontoDeHabilidade();
        }
        for (ProgressionProfile.ProgressionAttribute state : profile.attributes()) {
            Atributo atributo = availableAttributes.stream()
                    .filter(candidate -> key(candidate).equals(state.key()))
                    .findFirst()
                    .orElse(null);
            if (atributo == null) {
                continue;
            }
            AtributoJogador attribute = jogador.adicionarAtributo(atributo);
            attribute.setXpTotal(state.xp());
            attribute.setNivelAtual(state.level());
        }
    }

    private String key(Object value) {
        if (value instanceof com.josecjuniors.logossrv.core.atributo.domain.model.Atributo atributo) {
            return atributo.getId().getValue().toString();
        }
        if (value instanceof com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade habilidade) {
            return habilidade.getId().getValue().toString();
        }
        throw new IllegalArgumentException("Tipo de progressão não suportado");
    }
}
