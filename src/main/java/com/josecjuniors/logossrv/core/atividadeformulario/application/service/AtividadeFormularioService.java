package com.josecjuniors.logossrv.core.atividadeformulario.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeConfigSalvaEvent;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormularioId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.CampoFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.repository.RegraFatorXPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AtividadeFormularioService {

    private static final Logger logger = LoggerFactory.getLogger(AtividadeFormularioService.class);

    private final AtividadeConfigRepository atividadeConfigRepository;
    private final AtividadeFormularioRepository atividadeFormularioRepository;
    private final RegraFatorXPRepository regraFatorXPRepository;

    public AtividadeFormularioService(AtividadeConfigRepository atividadeConfigRepository, AtividadeFormularioRepository atividadeFormularioRepository, RegraFatorXPRepository regraFatorXPRepository) {
        this.atividadeConfigRepository = atividadeConfigRepository;
        this.atividadeFormularioRepository = atividadeFormularioRepository;
        this.regraFatorXPRepository = regraFatorXPRepository;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAtividadeConfigSalva(AtividadeConfigSalvaEvent event) {
        logger.info("Recebido evento para gerar formulário para a atividade ID: {}", event.atividadeConfigId().getValue());
        this.generateAndSaveFormulario(event);
    }

    public void generateAndSaveFormulario(AtividadeConfigSalvaEvent event) {
        AtividadeConfig atividadeConfig = atividadeConfigRepository.findById(event.atividadeConfigId())
                .orElseThrow(() -> new IllegalStateException("AtividadeConfig não encontrada para o evento. ID: " + event.atividadeConfigId().getValue()));

        List<RegraFatorXP> regras = regraFatorXPRepository.findRegrasByAtividadeConfigId(atividadeConfig.getId());

        List<CampoFormularioJson> campos = regras.stream()
                .map(regra -> {
                    FatorCalculo fator = regra.getFatorCalculo();
                    String placeholder = gerarPlaceholder(regra);
                    boolean obrigatorio = true;

                    return new CampoFormularioJson(
                            fator.getId().getValue(),
                            fator.getNome(),
                            fator.getUnidadeMedida(),
                            fator.getTipoInput(),
                            placeholder,
                            obrigatorio
                    );
                })
                .collect(Collectors.toList());

        AtividadeFormularioJson formularioJson = new AtividadeFormularioJson(
                atividadeConfig.getId().getValue(),
                atividadeConfig.getNome(),
                atividadeConfig.getDescricao(),
                campos
        );

        atividadeFormularioRepository.findByAtividadeConfigId(atividadeConfig.getId())
                .ifPresentOrElse(
                        formularioExistente -> {
                            logger.info("Atualizando formulário existente para a atividade ID: {}", atividadeConfig.getId().getValue());
                            formularioExistente.atualizar(formularioJson);
                            atividadeFormularioRepository.save(formularioExistente);
                        },
                        () -> {
                            logger.info("Criando novo formulário para a atividade ID: {}", atividadeConfig.getId().getValue());
                            AtividadeFormulario novoFormulario = new AtividadeFormulario(
                                    AtividadeFormularioId.generate(),
                                    atividadeConfig,
                                    formularioJson
                            );
                            atividadeFormularioRepository.save(novoFormulario);
                        }
                );

        logger.info("Processamento do formulário para a atividade ID: {} concluído.", event.atividadeConfigId().getValue());
    }

    private String gerarPlaceholder(RegraFatorXP regra) {
        Double min = regra.getPontoCorteMin();
        Double max = regra.getPontoCorteMax();

        if (min != null && max != null) {
            return String.format(Locale.US, "Ex: entre %.1f e %.1f", min, max);
        } else if (min != null) {
            return String.format(Locale.US, "Ex: acima de %.1f", min);
        } else if (max != null) {
            return String.format(Locale.US, "Ex: até %.1f", max);
        }
        return "";
    }
}
