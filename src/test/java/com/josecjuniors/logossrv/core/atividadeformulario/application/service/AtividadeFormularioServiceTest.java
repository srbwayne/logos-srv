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

    private AtividadeConfig testAtividade;

    @BeforeEach
    void setUp() {
        regraFatorXPRepository.deleteAll();
        regraDistribuicaoRepository.deleteAll();
        atividadeFormularioRepository.deleteAll();
        atividadeConfigRepository.deleteAll();
        atributoRepository.deleteAll();
        fatorCalculoRepository.deleteAll();

        testAtividade = atividadeConfigRepository.save(new AtividadeConfig(new AtividadeConfigId(), "Corrida", "Corrida de rua", 100, 10, null, null));
        Atributo resistencia = atributoRepository.save(new Atributo(new AtributoId(), "Resistência", null));
        RegraDistribuicaoAtividade regraDist = regraDistribuicaoRepository.save(new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), testAtividade, resistencia, 1.0));
        FatorCalculo distancia = fatorCalculoRepository.save(new FatorCalculo(FatorCalculoId.generate(), "Distância", "km", TipoInput.NUMERICO));
        regraFatorXPRepository.save(new RegraFatorXP(new RegraFatorXPId(), regraDist, distancia, 1.2, 5.0, 21.0));
    }

    @Test
    void deveGerarFormularioQuandoAtividadeConfigEhSalva() {
        // Arrange
        AtividadeConfigSalvaEvent event = new AtividadeConfigSalvaEvent(testAtividade.getId());

        // Act: Chama o método worker diretamente
        atividadeFormularioService.generateAndSaveFormulario(event);

        // Assert
        Optional<AtividadeFormulario> formularioOpt = atividadeFormularioRepository.findByAtividadeConfigId(testAtividade.getId());
        
        assertThat(formularioOpt).isPresent();
        
        AtividadeFormulario formulario = formularioOpt.get();
        assertThat(formulario.getVersao()).isEqualTo(1);
        assertThat(formulario.getFormularioJson()).isNotNull();
        assertThat(formulario.getFormularioJson().nomeAtividade()).isEqualTo("Corrida");
        assertThat(formulario.getFormularioJson().campos()).hasSize(1);
        
        var campo = formulario.getFormularioJson().campos().get(0);
        assertThat(campo.nome()).isEqualTo("Distância");
        assertThat(campo.tipoInput()).isEqualTo(TipoInput.NUMERICO);
        assertThat(campo.placeholder()).isEqualTo("Ex: entre 5.0 e 21.0");
        assertThat(campo.obrigatorio()).isTrue();
    }
}
