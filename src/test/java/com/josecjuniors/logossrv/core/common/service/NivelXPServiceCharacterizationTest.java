package com.josecjuniors.logossrv.core.common.service;

import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NivelXPServiceCharacterizationTest {

    private final NivelXPService service = new NivelXPService();

    @Test
    void calculaLimiarQuadraticoDeNivel() {
        assertThat(service.calcularXpParaProximoNivel(1)).isEqualTo(150);
        assertThat(service.calcularXpParaProximoNivel(2)).isEqualTo(450);
    }

    @Test
    void promoveNoLimiarGlobalEConservaXpBrutoAcumulado() {
        Jogador jogador = jogador();

        service.adicionarExperiencia(jogador, 150);

        assertThat(jogador.getNivelAtual()).isEqualTo(2);
        assertThat(jogador.getPontosHabilidade()).isEqualTo(2);
        assertThat(jogador.getXpTotal()).isEqualTo(150);
    }

    @Test
    void truncaXpLiquidoAntesDaTransicaoQuandoEstressePenaliza() {
        Jogador abaixoDoLimiar = jogador();
        abaixoDoLimiar.aplicarEstresse(30);
        service.adicionarExperiencia(abaixoDoLimiar, 176);

        assertThat(abaixoDoLimiar.getNivelAtual()).isEqualTo(1);
        assertThat(abaixoDoLimiar.getXpTotal()).isEqualTo(176);

        Jogador noLimiar = jogador();
        noLimiar.aplicarEstresse(30);
        service.adicionarExperiencia(noLimiar, 177);

        assertThat(noLimiar.getNivelAtual()).isEqualTo(2);
        assertThat(noLimiar.getPontosHabilidade()).isEqualTo(2);
        assertThat(noLimiar.getXpTotal()).isEqualTo(177);
    }

    private Jogador jogador() {
        Jogador jogador = new Jogador(JogadorId.generate(), null, "tester");
        jogador.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), jogador));
        return jogador;
    }
}
