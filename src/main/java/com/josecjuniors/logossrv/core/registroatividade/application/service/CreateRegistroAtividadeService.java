package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoNaoEncontradoException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeCommand;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeUseCase;
import com.josecjuniors.logossrv.core.registroatividade.domain.events.RegistroAtividadeCriadoEvent;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import com.josecjuniors.logossrv.core.progression.application.port.out.ActivityProgressionExecutionStore;
import com.josecjuniors.logossrv.core.progression.application.port.out.VersionedProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.FactKeyGeneration;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionFactKeyNotRepresentedException;

@Service
@Transactional
public class CreateRegistroAtividadeService implements CreateRegistroAtividadeUseCase {

    private final RegistroAtividadeRepository registroRepository;
    private final JogadorRepository jogadorRepository;
    private final AtividadeConfigRepository atividadeConfigRepository;
    private final FatorCalculoRepository fatorCalculoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final VersionedProgressionConfigurationResolver versionedResolver;
    private final ActivityProgressionExecutionStore executionStore;

    public CreateRegistroAtividadeService(RegistroAtividadeRepository registroRepository, JogadorRepository jogadorRepository, AtividadeConfigRepository atividadeConfigRepository, FatorCalculoRepository fatorCalculoRepository, ApplicationEventPublisher eventPublisher) {
        this(registroRepository, jogadorRepository, atividadeConfigRepository, fatorCalculoRepository, eventPublisher, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CreateRegistroAtividadeService(RegistroAtividadeRepository registroRepository, JogadorRepository jogadorRepository, AtividadeConfigRepository atividadeConfigRepository, FatorCalculoRepository fatorCalculoRepository, ApplicationEventPublisher eventPublisher, VersionedProgressionConfigurationResolver versionedResolver, ActivityProgressionExecutionStore executionStore) {
        this.registroRepository = registroRepository;
        this.jogadorRepository = jogadorRepository;
        this.atividadeConfigRepository = atividadeConfigRepository;
        this.fatorCalculoRepository = fatorCalculoRepository;
        this.eventPublisher = eventPublisher;
        this.versionedResolver = versionedResolver;
        this.executionStore = executionStore;
    }

    @Override
    public void create(CreateRegistroAtividadeCommand command) {
        Jogador jogador = jogadorRepository.findByUserEmail(command.userEmail())
                .orElseThrow(JogadorNaoEncontradoException::new);
        AtividadeConfig atividadeConfig = atividadeConfigRepository.findById(new AtividadeConfigId(command.atividadeConfigId()))
                .orElseThrow(AtividadeConfigNaoEncontradaException::new);

        RegistroAtividade novoRegistro = new RegistroAtividade(RegistroAtividadeId.generate(), jogador, atividadeConfig, command.dataHoraInicio(), command.dataHoraFim());

        command.detalhes().forEach(detalhe -> {
            FatorCalculo fator = fatorCalculoRepository.findById(new FatorCalculoId(detalhe.fatorCalculoId()))
                    .orElseThrow(() -> new FatorCalculoNaoEncontradoException(detalhe.valor()));
            novoRegistro.adicionarDetalhe(fator, detalhe.valor());
        });

        RegistroAtividade registroSalvo = registroRepository.save(novoRegistro);

        if (versionedResolver != null && executionStore != null) {
            var resolvedOptional = versionedResolver.resolveVersioned(new ProgressionConfigurationReference(atividadeConfig.getId().getValue()));
            if (resolvedOptional.isEmpty()) {
                eventPublisher.publishEvent(new RegistroAtividadeCriadoEvent(registroSalvo.getId()));
                return;
            }
            var resolved = resolvedOptional.get();
            var facts = new ProgressionFact(novoRegistro.getDetalhes().stream()
                    .map(d -> factDetailFor(d, resolved))
                    .filter(java.util.Objects::nonNull)
                    .toList());
            var identity = ActivityProgressionAdapter.identity(registroSalvo.getId().getValue());
            var fingerprint = com.josecjuniors.logossrv.core.progression.application.service.ProgressionExecutionFingerprint.ofFrozen(
                    identity, new ExternalSubjectReference("logos", jogador.getId().getValue().toString()),
                    new ExternalProgressionConfigurationReference("activity-" + atividadeConfig.getId().getValue(), 1), facts,
                    resolved.configurationVersionId(), resolved.skillPolicyVersionId());
            try {
                executionStore.create(identity, fingerprint, jogador.getUser().getId().getValue(), facts, resolved,
                        "activity-" + atividadeConfig.getId().getValue(), 1);
            } catch (RuntimeException exception) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw exception;
            }
        }

        eventPublisher.publishEvent(new RegistroAtividadeCriadoEvent(registroSalvo.getId()));
    }

    private ProgressionFact.Detail factDetailFor(com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeDetalhe detail,
                                                  com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration resolved) {
        if (!detail.getFatorCalculo().getTipoInput().ehValorNumerico()) {
            return null;
        }
        String legacyKey = detail.getFatorCalculo().getId().getValue().toString();
        String semanticKey = detail.getFatorCalculo().getSemanticKey();
        String effectiveKey = resolved.factKeyGeneration() == FactKeyGeneration.LEGACY_UUID
                ? resolved.numericFactorKeys().contains(legacyKey) ? legacyKey : null
                : semanticKey != null && resolved.numericFactorKeys().contains(semanticKey) ? semanticKey : null;
        if (effectiveKey == null) {
            throw new ProgressionFactKeyNotRepresentedException(legacyKey);
        }
        return new ProgressionFact.Detail(effectiveKey, Double.parseDouble(detail.getValorRegistrado()));
    }
}
