package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.common.service.NivelXPService;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.HabilidadeJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.HabilidadeJogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidade;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidadeId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import com.josecjuniors.logossrv.core.registroatividade.domain.events.RegistroAtividadeCriadoEvent;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.enums.StatusProcessamento;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessarRegistroAtividadeServiceCharacterizationTest {

    private RegistroAtividadeRepository registroRepository;
    private JogadorRepository jogadorRepository;
    private ProcessarRegistroAtividadeService service;

    @BeforeEach
    void setUp() {
        registroRepository = mock(RegistroAtividadeRepository.class);
        jogadorRepository = mock(JogadorRepository.class);
        service = new ProcessarRegistroAtividadeService(registroRepository, jogadorRepository, new NivelXPService());
    }

    @Test
    void aplicaMultiplicadorDeXpNosLimitesInclusivosEPercentualDaDistribuicao() {
        Jogador jogador = jogador();
        Atributo atributo = atributo("Forca");
        AtividadeConfig atividade = atividade(100, 10);
        RegraDistribuicaoAtividade distribuicao = distribuicao(atividade, atributo, .25);
        FatorCalculo fator = fator("peso");
        distribuicao.adicionarRegraFatorXPS(regraXp(distribuicao, fator, 2.0, 80.0, 120.0));
        atividade.adicionarRegraDistribuicao(distribuicao);

        RegistroAtividade registroNoMinimo = registro(jogador, atividade);
        registroNoMinimo.adicionarDetalhe(fator, "80");
        processar(registroNoMinimo);

        assertThat(registroNoMinimo.getXpGanhoFinal()).isEqualTo(50);
        assertThat(jogador.getAtributo(atributo).getXpTotal()).isEqualTo(50);

        Jogador outroJogador = jogador();
        RegistroAtividade registroNoMaximo = registro(outroJogador, atividade);
        registroNoMaximo.adicionarDetalhe(fator, "120");
        processar(registroNoMaximo);

        assertThat(registroNoMaximo.getXpGanhoFinal()).isEqualTo(50);
    }

    @Test
    void aplicaDivisaoQuandoRegraDeXpNaoCasaComOValor() {
        Jogador jogador = jogador();
        Atributo atributo = atributo("Forca");
        AtividadeConfig atividade = atividade(100, 10);
        RegraDistribuicaoAtividade distribuicao = distribuicao(atividade, atributo, .5);
        FatorCalculo fator = fator("peso");
        distribuicao.adicionarRegraFatorXPS(regraXp(distribuicao, fator, 2.0, 80.0, 120.0));
        atividade.adicionarRegraDistribuicao(distribuicao);
        RegistroAtividade registro = registro(jogador, atividade);
        registro.adicionarDetalhe(fator, "79.9");

        processar(registro);

        assertThat(registro.getXpGanhoFinal()).isEqualTo(25);
        assertThat(jogador.getAtributo(atributo).getXpTotal()).isEqualTo(25);
    }

    @Test
    void trataCortesNulosComoIntervaloAbertoETruncaResultadoFracionario() {
        Jogador jogador = jogador();
        Atributo atributo = atributo("Forca");
        AtividadeConfig atividade = atividade(101, 11);
        RegraDistribuicaoAtividade distribuicao = distribuicao(atividade, atributo, .25);
        FatorCalculo fator = fator("peso");
        distribuicao.adicionarRegraFatorXPS(regraXp(distribuicao, fator, 1.5, null, null));
        atividade.adicionarRegraDistribuicao(distribuicao);
        RegistroAtividade registro = registro(jogador, atividade);
        registro.adicionarDetalhe(fator, "999");

        processar(registro);

        assertThat(registro.getXpGanhoFinal()).isEqualTo(37);
        assertThat(jogador.getAtributo(atributo).getXpTotal()).isEqualTo(37);
        assertThat(registro.getEstresseGerado()).isEqualTo(11);
    }

    @Test
    void acumulaDetalhesEFatoresNaMesmaDistribuicaoEAplicaEstressePorDetalhe() {
        Jogador jogador = jogador();
        Atributo atributo = atributo("Forca");
        AtividadeConfig atividade = atividade(100, 10);
        RegraDistribuicaoAtividade distribuicao = distribuicao(atividade, atributo, .5);
        FatorCalculo peso = fator("peso");
        FatorCalculo repeticoes = fator("repeticoes");
        distribuicao.adicionarRegraFatorXPS(regraXp(distribuicao, peso, 1.0, null, null));
        distribuicao.adicionarRegraFatorXPS(regraXp(distribuicao, repeticoes, 1.0, null, null));
        distribuicao.adicionarRegraFatorEstresses(regraEstresse(distribuicao, 2.0, 0.0, null, TipoFatorEstresse.POSITIVO));
        atividade.adicionarRegraDistribuicao(distribuicao);
        RegistroAtividade registro = registro(jogador, atividade);
        registro.adicionarDetalhe(peso, "10");
        registro.adicionarDetalhe(repeticoes, "5");

        processar(registro);

        assertThat(registro.getXpGanhoFinal()).isEqualTo(100);
        assertThat(jogador.getAtributo(atributo).getXpTotal()).isEqualTo(100);
        assertThat(registro.getEstresseGerado()).isEqualTo(20);
    }

    @Test
    void distribuiXpEntreDoisAtributosSemAlterarTotalGlobal() {
        Jogador jogador = jogador();
        Atributo forca = atributo("Forca");
        Atributo foco = atributo("Foco");
        AtividadeConfig atividade = atividade(100, 0);
        FatorCalculo fator = fator("peso");
        RegraDistribuicaoAtividade distribuicaoForca = distribuicao(atividade, forca, .5);
        RegraDistribuicaoAtividade distribuicaoFoco = distribuicao(atividade, foco, .5);
        distribuicaoForca.adicionarRegraFatorXPS(regraXp(distribuicaoForca, fator, 1.0, null, null));
        distribuicaoFoco.adicionarRegraFatorXPS(regraXp(distribuicaoFoco, fator, 1.0, null, null));
        atividade.adicionarRegraDistribuicao(distribuicaoForca);
        atividade.adicionarRegraDistribuicao(distribuicaoFoco);
        RegistroAtividade registro = registro(jogador, atividade);
        registro.adicionarDetalhe(fator, "1");

        processar(registro);

        assertThat(registro.getXpGanhoFinal()).isEqualTo(100);
        assertThat(jogador.getAtributo(forca).getXpTotal()).isEqualTo(50);
        assertThat(jogador.getAtributo(foco).getXpTotal()).isEqualTo(50);
        assertThat(jogador.getXpTotal()).isEqualTo(100);
    }

    @Test
    void somaFatoresDeEstressePositivosENegativosEAoPersistirMarcaORegistroProcessado() {
        Jogador jogador = jogador();
        Atributo atributo = atributo("Forca");
        AtividadeConfig atividade = atividade(10, 10);
        RegraDistribuicaoAtividade distribuicao = distribuicao(atividade, atributo, 1.0);
        FatorCalculo fator = fator("peso");
        distribuicao.adicionarRegraFatorEstresses(regraEstresse(distribuicao, 2.0, 0.0, null, TipoFatorEstresse.POSITIVO));
        distribuicao.adicionarRegraFatorEstresses(regraEstresse(distribuicao, 1.0, 0.0, null, TipoFatorEstresse.NEGATIVO));
        atividade.adicionarRegraDistribuicao(distribuicao);
        RegistroAtividade registro = registro(jogador, atividade);
        registro.adicionarDetalhe(fator, "1");

        processar(registro);

        assertThat(registro.getStatusProcessamento()).isEqualTo(StatusProcessamento.PROCESSADO);
        assertThat(registro.getXpGanhoFinal()).isEqualTo(10);
        assertThat(registro.getEstresseGerado()).isEqualTo(5);
        assertThat(jogador.getEstresseGlobal().getPontuacaoAtual()).isEqualTo(5);
        verify(jogadorRepository).save(jogador);
        verify(registroRepository).save(registro);
    }

    @Test
    void adicionaBonusDeHabilidadeAoXpDoAtributoMasNaoAoXpGlobal() {
        Jogador jogador = jogador();
        Atributo atributo = atributo("Forca");
        AtividadeConfig atividade = atividade(100, 0);
        RegraDistribuicaoAtividade distribuicao = distribuicao(atividade, atributo, 1.0);
        FatorCalculo fator = fator("peso");
        distribuicao.adicionarRegraFatorXPS(regraXp(distribuicao, fator, 1.0, null, null));
        atividade.adicionarRegraDistribuicao(distribuicao);

        Habilidade habilidade = new Habilidade(new HabilidadeId(), "Musculacao", "");
        habilidade.getRegrasDistribuicao().add(new RegraDistribuicaoHabilidade(new RegraDistribuicaoHabilidadeId(), habilidade, atributo, 10.0));
        HabilidadeJogador habilidadeJogador = new HabilidadeJogador(new HabilidadeJogadorId(), jogador, habilidade);
        habilidadeJogador.evoluirNivel();
        habilidadeJogador.evoluirNivel();
        jogador.getHabilidades().add(habilidadeJogador);

        RegistroAtividade registro = registro(jogador, atividade);
        registro.adicionarDetalhe(fator, "1");
        processar(registro);

        assertThat(registro.getXpGanhoFinal()).isEqualTo(100);
        assertThat(jogador.getXpTotal()).isEqualTo(100);
        assertThat(jogador.getAtributo(atributo).getXpTotal()).isEqualTo(101);
    }

    private void processar(RegistroAtividade registro) {
        when(registroRepository.findByIdWithDetails(registro.getId())).thenReturn(Optional.of(registro));
        service.processarEvento(new RegistroAtividadeCriadoEvent(registro.getId()));
    }

    private Jogador jogador() {
        Jogador jogador = new Jogador(JogadorId.generate(), null, "tester");
        jogador.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), jogador));
        return jogador;
    }

    private AtividadeConfig atividade(int xpBase, int estresseBase) {
        return new AtividadeConfig(AtividadeConfigId.generate(), "atividade", null, xpBase, estresseBase, null, null);
    }

    private Atributo atributo(String nome) {
        return new Atributo(new AtributoId(), nome, "");
    }

    private FatorCalculo fator(String nome) {
        return new FatorCalculo(FatorCalculoId.generate(), nome, "", TipoInput.NUMERICO);
    }

    private RegraDistribuicaoAtividade distribuicao(AtividadeConfig atividade, Atributo atributo, double peso) {
        return new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), atividade, atributo, peso);
    }

    private RegraFatorXP regraXp(RegraDistribuicaoAtividade distribuicao, FatorCalculo fator, double multiplicador, Double minimo, Double maximo) {
        return new RegraFatorXP(new RegraFatorXPId(), distribuicao, fator, multiplicador, minimo, maximo);
    }

    private RegraFatorEstresse regraEstresse(RegraDistribuicaoAtividade distribuicao, double multiplicador, Double minimo, Double maximo, TipoFatorEstresse tipo) {
        return new RegraFatorEstresse(new RegraFatorEstresseId(), distribuicao, multiplicador, minimo, maximo, tipo);
    }

    private RegistroAtividade registro(Jogador jogador, AtividadeConfig atividade) {
        return new RegistroAtividade(RegistroAtividadeId.generate(), jogador, atividade, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    }
}
