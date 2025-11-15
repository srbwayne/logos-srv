package com.josecjuniors.logossrv.core.atividadeformulario.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.repository.RegraFatorXPRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class AtividadeFormularioServiceTest {

    @Autowired
    private AtividadeFormularioService atividadeFormularioService;

    @Autowired
    private AtividadeFormularioRepository atividadeFormularioRepository;
    @Autowired
    private AtividadeConfigRepository atividadeConfigRepository;
    @Autowired
    private AtributoRepository atributoRepository;
    @Autowired
    private RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository;
    @Autowired
    private FatorCalculoRepository fatorCalculoRepository;
    @Autowired
    private RegraFatorXPRepository regraFatorXPRepository;

    @BeforeEach
    void setUp() {
        regraFatorXPRepository.deleteAll();
        regraDistribuicaoRepository.deleteAll();
        atividadeFormularioRepository.deleteAll();
        atividadeConfigRepository.deleteAll();
        atributoRepository.deleteAll();
        fatorCalculoRepository.deleteAll();
    }

    @Test
    void deveGerarFormularioQuandoAtividadeConfigEhSalva() {
        // Arrange
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida", "Corrida de rua", 100, 10, null, null));
        Atributo resistencia = atributoRepository.save(new Atributo(new AtributoId(), "Resistência", null));
        RegraDistribuicaoAtividade regraDist = regraDistribuicaoRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), atividade, resistencia, 1.0));
        FatorCalculo distancia = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Distância", "km", TipoInput.NUMERICO));
        regraFatorXPRepository.save(new RegraFatorXP(new RegraFatorXPId(), regraDist, distancia, 1.2, 5.0, 21.0));
        AtividadeConfigSalvaEvent event = new AtividadeConfigSalvaEvent(atividade.getId());

        // Act
        atividadeFormularioService.generateAndSaveFormulario(event);

        // Assert
        Optional<AtividadeFormulario> formularioOpt = atividadeFormularioRepository.findByAtividadeConfigId(atividade.getId());
        assertThat(formularioOpt).isPresent();
        AtividadeFormulario formulario = formularioOpt.get();
        assertThat(formulario.getVersao()).isEqualTo(1);
        assertThat(formulario.getFormularioJson().campos()).hasSize(1);
        assertThat(formulario.getFormularioJson().campos().get(0).nome()).isEqualTo("Distância");
    }

    @Test
    void deveRegerarFormularioQuandoRegraEhAlterada() {
        // Arrange: Setup com 2 regras de XP
        AtividadeConfig atividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Musculação", null, 120, 15, null, null));
        Atributo forca = atributoRepository.save(new Atributo(new AtributoId(), "Força", null));
        RegraDistribuicaoAtividade regraDist = regraDistribuicaoRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), atividade, forca, 1.0));
        FatorCalculo peso = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Peso Levantado", "kg", TipoInput.NUMERICO));
        FatorCalculo repeticoes = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Repetições", "reps", TipoInput.NUMERICO));
        regraFatorXPRepository.save(new RegraFatorXP(new RegraFatorXPId(), regraDist, peso, 1.0, 50.0, null));
        RegraFatorXP regraRepeticoes = regraFatorXPRepository.save(new RegraFatorXP(new RegraFatorXPId(), regraDist, repeticoes, 0.5, 8.0, 12.0));
        AtividadeConfigSalvaEvent event = new AtividadeConfigSalvaEvent(atividade.getId());

        // Act 1: Gera o formulário inicial
        atividadeFormularioService.generateAndSaveFormulario(event);

        // Assert 1: Verifica a criação inicial
        Optional<AtividadeFormulario> formularioInicialOpt = atividadeFormularioRepository.findByAtividadeConfigId(atividade.getId());
        assertThat(formularioInicialOpt).isPresent();
        assertThat(formularioInicialOpt.get().getVersao()).isEqualTo(1);
        assertThat(formularioInicialOpt.get().getFormularioJson().campos()).hasSize(2);

        // Act 2: Remove uma regra e dispara o evento novamente
        regraFatorXPRepository.deleteById(regraRepeticoes.getId());
        atividadeFormularioService.generateAndSaveFormulario(event); // Simula o evento sendo disparado novamente

        // Assert 2: Verifica a regeneração
        Optional<AtividadeFormulario> formularioAtualizadoOpt = atividadeFormularioRepository.findByAtividadeConfigId(atividade.getId());
        assertThat(formularioAtualizadoOpt).isPresent();
        AtividadeFormulario formularioAtualizado = formularioAtualizadoOpt.get();
        assertThat(formularioAtualizado.getVersao()).isEqualTo(2);
        assertThat(formularioAtualizado.getFormularioJson().campos()).hasSize(1);
        assertThat(formularioAtualizado.getFormularioJson().campos().get(0).nome()).isEqualTo("Peso Levantado");
    }
}
