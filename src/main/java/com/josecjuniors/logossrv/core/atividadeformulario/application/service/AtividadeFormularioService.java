package com.josecjuniors.logossrv.core.atividadeformulario.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.events.AtividadeCatalogoSalvoEvent;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.application.port.in.ReplaceAtividadeFormularioCommand;
import com.josecjuniors.logossrv.core.atividadeformulario.application.port.in.ReplaceAtividadeFormularioUseCase;
import com.josecjuniors.logossrv.core.atividadeformulario.application.dto.AtividadeFormularioDto;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.exception.AtividadeFormularioConflitoException;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormularioId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.CampoFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoNaoEncontradoException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AtividadeFormularioService implements ReplaceAtividadeFormularioUseCase {
    private final AtividadeConfigRepository atividadeConfigRepository;
    private final AtividadeFormularioRepository atividadeFormularioRepository;
    private final FatorCalculoRepository fatorCalculoRepository;

    public AtividadeFormularioService(AtividadeConfigRepository atividadeConfigRepository,
                                      AtividadeFormularioRepository atividadeFormularioRepository,
                                      FatorCalculoRepository fatorCalculoRepository) {
        this.atividadeConfigRepository = atividadeConfigRepository;
        this.atividadeFormularioRepository = atividadeFormularioRepository;
        this.fatorCalculoRepository = fatorCalculoRepository;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAtividadeCatalogoSalvo(AtividadeCatalogoSalvoEvent event) {
        AtividadeConfig atividade = atividadeConfigRepository.findById(event.atividadeConfigId())
                .orElseThrow(() -> new IllegalStateException("AtividadeConfig não encontrada para o evento."));
        atividadeFormularioRepository.findByAtividadeConfigIdForUpdate(atividade.getId())
                .ifPresentOrElse(formulario -> {
                    formulario.refreshActivityMetadata(atividade.getNome(), atividade.getDescricao());
                    atividadeFormularioRepository.save(formulario);
                }, () -> atividadeFormularioRepository.save(new AtividadeFormulario(
                        AtividadeFormularioId.generate(), atividade,
                        new AtividadeFormularioJson(atividade.getId().getValue(), atividade.getNome(),
                                atividade.getDescricao(), List.of()))));
    }

    @Override
    @Transactional
    public AtividadeFormularioDto replace(ReplaceAtividadeFormularioCommand command) {
        AtividadeConfig atividade = atividadeConfigRepository.findById(command.atividadeConfigId())
                .orElseThrow(AtividadeConfigNaoEncontradaException::new);
        Set<FatorCalculoId> identities = new HashSet<>();
        List<CampoFormularioJson> campos = command.campos().stream().map(campo -> {
            if (campo.fatorCalculoId() == null || !identities.add(new FatorCalculoId(campo.fatorCalculoId()))) {
                throw new IllegalArgumentException("form fields must contain unique FactDefinition ids");
            }
            FatorCalculo fator = fatorCalculoRepository.findById(new FatorCalculoId(campo.fatorCalculoId()))
                    .orElseThrow(() -> new FatorCalculoNaoEncontradoException(campo.fatorCalculoId().toString()));
            return new CampoFormularioJson(fator.getId().getValue(), fator.getNome(), fator.getUnidadeMedida(),
                    fator.getTipoInput(), campo.placeholder() == null ? "" : campo.placeholder(), true);
        }).toList();
        AtividadeFormularioJson json = new AtividadeFormularioJson(atividade.getId().getValue(), atividade.getNome(),
                atividade.getDescricao(), campos);
        AtividadeFormulario formulario = atividadeFormularioRepository.findByAtividadeConfigIdForUpdate(atividade.getId()).orElse(null);
        if (formulario == null) {
            if (command.expectedVersion() != 0) {
                throw new AtividadeFormularioConflitoException("stale activity form version");
            }
            AtividadeFormulario created = atividadeFormularioRepository.save(new AtividadeFormulario(AtividadeFormularioId.generate(), atividade, json));
            return new AtividadeFormularioDto(created.getFormularioJson(), created.getVersao());
        }
        if (formulario.getVersao() != command.expectedVersion()) {
            throw new AtividadeFormularioConflitoException("stale activity form version");
        }
        formulario.replaceCaptureDefinition(json);
        AtividadeFormulario saved = atividadeFormularioRepository.save(formulario);
        return new AtividadeFormularioDto(saved.getFormularioJson(), saved.getVersao());
    }
}
