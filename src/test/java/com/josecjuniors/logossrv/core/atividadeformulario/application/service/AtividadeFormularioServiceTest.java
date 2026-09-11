package com.josecjuniors.logossrv.core.atividadeformulario.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.application.port.in.ReplaceAtividadeFormularioCommand;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.exception.AtividadeFormularioConflitoException;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class AtividadeFormularioServiceTest {
    @Autowired private AtividadeFormularioService service;
    @Autowired private AtividadeFormularioRepository formularioRepository;
    @Autowired private AtividadeConfigRepository atividadeRepository;
    @Autowired private FatorCalculoRepository fatorRepository;

    @BeforeEach
    void setUp() {
        formularioRepository.deleteAll();
        atividadeRepository.deleteAll();
        fatorRepository.deleteAll();
    }

    @Test
    void replacesFormWithFactThatHasNoXpRuleAndKeepsCallerPlaceholder() {
        AtividadeConfig atividade = atividadeRepository.save(new AtividadeConfig(
                new AtividadeConfigId(), "Corrida", "Rua", 100, 10, null, null));
        FatorCalculo distancia = fatorRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Distância", "km", TipoInput.NUMERICO));

        service.replace(new ReplaceAtividadeFormularioCommand(atividade.getId(), 0,
                List.of(new ReplaceAtividadeFormularioCommand.Campo(distancia.getId().getValue(), "Distância percorrida"))));

        AtividadeFormulario formulario = formularioRepository.findByAtividadeConfigId(atividade.getId()).orElseThrow();
        assertThat(formulario.getVersao()).isEqualTo(1);
        assertThat(formulario.getFormularioJson().campos()).singleElement().satisfies(campo -> {
            assertThat(campo.fatorCalculoId()).isEqualTo(distancia.getId().getValue());
            assertThat(campo.placeholder()).isEqualTo("Distância percorrida");
            assertThat(campo.obrigatorio()).isTrue();
        });
    }

    @Test
    void staleReplacementIsRejectedAndPersistedFormIsUnchanged() {
        AtividadeConfig atividade = atividadeRepository.save(new AtividadeConfig(
                new AtividadeConfigId(), "Foco", null, 10, 1, null, null));
        FatorCalculo minutos = fatorRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Minutos", "min", TipoInput.NUMERICO));
        service.replace(new ReplaceAtividadeFormularioCommand(atividade.getId(), 0,
                List.of(new ReplaceAtividadeFormularioCommand.Campo(minutos.getId().getValue(), "minutos"))));

        assertThatThrownBy(() -> service.replace(new ReplaceAtividadeFormularioCommand(atividade.getId(), 0, List.of())))
                .isInstanceOf(AtividadeFormularioConflitoException.class);
        assertThat(formularioRepository.findByAtividadeConfigId(atividade.getId()).orElseThrow()
                .getFormularioJson().campos()).hasSize(1);
    }

    @Test
    void catalogMetadataChangePreservesExplicitCaptureFields() {
        AtividadeConfig atividade = atividadeRepository.save(new AtividadeConfig(
                new AtividadeConfigId(), "Leitura", "Inicial", 10, 1, null, null));
        FatorCalculo paginas = fatorRepository.save(new FatorCalculo(
                FatorCalculoId.generate(), "Páginas", "pág", TipoInput.NUMERICO));
        service.replace(new ReplaceAtividadeFormularioCommand(atividade.getId(), 0,
                List.of(new ReplaceAtividadeFormularioCommand.Campo(paginas.getId().getValue(), "páginas"))));

        atividade.atualizar("Leitura diária", "Atualizada", 10, 1, null, null);
        service.onAtividadeCatalogoSalvo(new com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeCatalogoSalvoEvent(atividade.getId()));

        AtividadeFormulario formulario = formularioRepository.findByAtividadeConfigId(atividade.getId()).orElseThrow();
        assertThat(formulario.getFormularioJson().nomeAtividade()).isEqualTo("Leitura diária");
        assertThat(formulario.getFormularioJson().campos()).hasSize(1);
        assertThat(formulario.getFormularioJson().campos().get(0).placeholder()).isEqualTo("páginas");
    }
}
