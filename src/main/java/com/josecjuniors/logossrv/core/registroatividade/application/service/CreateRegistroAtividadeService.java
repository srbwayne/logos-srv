package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.exception.AtividadeFormularioConfiguracaoException;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.exception.AtividadeFormularioValidacaoException;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.CampoFormularioJson;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeCommand;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeUseCase;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.RegistroAtividadeDetalheCommand;
import com.josecjuniors.logossrv.core.registroatividade.domain.events.RegistroAtividadeCriadoEvent;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import com.josecjuniors.logossrv.core.progression.application.port.out.ActivityProgressionExecutionStore;
import com.josecjuniors.logossrv.core.progression.application.port.out.VersionedProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.FactKeyGeneration;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionFactKeyNotRepresentedException;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionConfigurationNotActiveException;

@Service
@Transactional
public class CreateRegistroAtividadeService implements CreateRegistroAtividadeUseCase {

    private static final Pattern NUMERIC_VALUE = Pattern.compile("^[+-]?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?$");
    private static final Pattern UUID_VALUE = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final int MAX_REGISTERED_VALUE_LENGTH = 100;

    private final RegistroAtividadeRepository registroRepository;
    private final JogadorRepository jogadorRepository;
    private final AtividadeConfigRepository atividadeConfigRepository;
    private final AtividadeFormularioRepository atividadeFormularioRepository;
    private final FatorCalculoRepository fatorCalculoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final VersionedProgressionConfigurationResolver versionedResolver;
    private final ActivityProgressionExecutionStore executionStore;

    public CreateRegistroAtividadeService(RegistroAtividadeRepository registroRepository, JogadorRepository jogadorRepository, AtividadeConfigRepository atividadeConfigRepository, FatorCalculoRepository fatorCalculoRepository, ApplicationEventPublisher eventPublisher) {
        this(registroRepository, jogadorRepository, atividadeConfigRepository, null, fatorCalculoRepository, eventPublisher, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CreateRegistroAtividadeService(RegistroAtividadeRepository registroRepository,
                                          JogadorRepository jogadorRepository,
                                          AtividadeConfigRepository atividadeConfigRepository,
                                          AtividadeFormularioRepository atividadeFormularioRepository,
                                          FatorCalculoRepository fatorCalculoRepository,
                                          ApplicationEventPublisher eventPublisher,
                                          VersionedProgressionConfigurationResolver versionedResolver,
                                          ActivityProgressionExecutionStore executionStore) {
        this.registroRepository = registroRepository;
        this.jogadorRepository = jogadorRepository;
        this.atividadeConfigRepository = atividadeConfigRepository;
        this.atividadeFormularioRepository = atividadeFormularioRepository;
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

        AtividadeFormulario formulario = requireFormulario(atividadeConfig);
        validateFormVersion(command.formVersion(), formulario.getVersao());
        List<RegistroAtividadeDetalheCommand> detalhes = command.detalhes() == null ? List.of() : command.detalhes();
        Map<UUID, FatorCalculo> fatores = validateFormInput(formulario, detalhes);

        RegistroAtividade registroSalvo;
        if (versionedResolver != null && executionStore != null) {
            var resolvedOptional = versionedResolver.resolveVersioned(new ProgressionConfigurationReference(atividadeConfig.getId().getValue()));
            var resolved = resolvedOptional.orElseThrow(ProgressionConfigurationNotActiveException::new);
            validateProgressionCompatibility(formulario, fatores, resolved);
            RegistroAtividade novoRegistro = new RegistroAtividade(RegistroAtividadeId.generate(), jogador, atividadeConfig, command.dataHoraInicio(), command.dataHoraFim());
            detalhes.forEach(detalhe -> novoRegistro.adicionarDetalhe(fatores.get(detalhe.fatorCalculoId()), detalhe.valor()));
            var facts = new ProgressionFact(novoRegistro.getDetalhes().stream()
                    .map(d -> factDetailFor(d, resolved))
                    .filter(java.util.Objects::nonNull)
                    .toList());
            var identity = ActivityProgressionAdapter.identity(novoRegistro.getId().getValue());
            var fingerprint = com.josecjuniors.logossrv.core.progression.application.service.ProgressionExecutionFingerprint.ofFrozen(
                    identity, new ExternalSubjectReference("logos", jogador.getId().getValue().toString()),
                    new ExternalProgressionConfigurationReference("activity-" + atividadeConfig.getId().getValue(), 1), facts,
                    resolved.configurationVersionId(), resolved.skillPolicyVersionId());
            registroSalvo = registroRepository.save(novoRegistro);
            try {
                executionStore.create(identity, fingerprint, jogador.getUser().getId().getValue(), facts, resolved,
                        "activity-" + atividadeConfig.getId().getValue(), 1);
            } catch (RuntimeException exception) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw exception;
            }
        } else {
            RegistroAtividade novoRegistro = new RegistroAtividade(RegistroAtividadeId.generate(), jogador, atividadeConfig, command.dataHoraInicio(), command.dataHoraFim());
            detalhes.forEach(detalhe -> novoRegistro.adicionarDetalhe(fatores.get(detalhe.fatorCalculoId()), detalhe.valor()));
            registroSalvo = registroRepository.save(novoRegistro);
        }
        eventPublisher.publishEvent(new RegistroAtividadeCriadoEvent(registroSalvo.getId()));
    }

    private AtividadeFormulario requireFormulario(AtividadeConfig atividadeConfig) {
        if (atividadeFormularioRepository == null) {
            throw new AtividadeFormularioConfiguracaoException("ACTIVITY_FORM_NOT_AVAILABLE", "activity form repository is not configured");
        }
        return atividadeFormularioRepository.findByAtividadeConfigId(atividadeConfig.getId())
                .orElseThrow(() -> new AtividadeFormularioConfiguracaoException(
                        "ACTIVITY_FORM_NOT_AVAILABLE", "canonical activity form is not available"));
    }

    private void validateFormVersion(Integer submittedVersion, int currentVersion) {
        if (submittedVersion == null) {
            throw new AtividadeFormularioValidacaoException(
                    "ACTIVITY_FORM_VERSION_REQUIRED", "formVersion is required");
        }
        if (submittedVersion != currentVersion) {
            throw new AtividadeFormularioConfiguracaoException(
                    "ACTIVITY_FORM_VERSION_CONFLICT", "activity form version is stale");
        }
    }

    private Map<UUID, FatorCalculo> validateFormInput(AtividadeFormulario formulario,
                                                       List<RegistroAtividadeDetalheCommand> detalhes) {
        List<CampoFormularioJson> campos = formulario.getFormularioJson().campos();
        Set<UUID> formIds = campos.stream().map(CampoFormularioJson::fatorCalculoId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> submittedIds = new HashSet<>();
        for (RegistroAtividadeDetalheCommand detalhe : detalhes) {
            if (detalhe == null || detalhe.fatorCalculoId() == null || !submittedIds.add(detalhe.fatorCalculoId())) {
                throw new AtividadeFormularioValidacaoException(
                        "ACTIVITY_FORM_DUPLICATE_FIELD", "submitted activity form fields must be unique");
            }
            if (!formIds.contains(detalhe.fatorCalculoId())) {
                throw new AtividadeFormularioValidacaoException(
                        "ACTIVITY_FORM_FIELD_NOT_ALLOWED", "submitted field is not part of the activity form");
            }
        }
        for (UUID formId : formIds) {
            if (!submittedIds.contains(formId)) {
                throw new AtividadeFormularioValidacaoException(
                        "ACTIVITY_FORM_REQUIRED_FIELD_MISSING", "required activity form field is missing");
            }
        }

        Map<UUID, FatorCalculo> fatores = new HashMap<>();
        Map<UUID, CampoFormularioJson> camposById = new HashMap<>();
        campos.forEach(campo -> camposById.put(campo.fatorCalculoId(), campo));
        for (UUID id : submittedIds) {
            FatorCalculo fator = fatorCalculoRepository.findById(new FatorCalculoId(id)).orElseThrow(() ->
                    new AtividadeFormularioConfiguracaoException(
                            "ACTIVITY_FORM_FACT_DEFINITION_MISMATCH", "activity form references a missing FactDefinition"));
            CampoFormularioJson campo = camposById.get(id);
            if (fator.getTipoInput() != campo.tipoInput()) {
                throw new AtividadeFormularioConfiguracaoException(
                        "ACTIVITY_FORM_FACT_DEFINITION_MISMATCH", "activity form and FactDefinition input types differ");
            }
            fatores.put(id, fator);
            validateValue(campo.tipoInput(), detalhes.stream()
                    .filter(detalhe -> id.equals(detalhe.fatorCalculoId()))
                    .findFirst().orElseThrow().valor());
        }
        return fatores;
    }

    private void validateValue(TipoInput tipoInput, String value) {
        if (value == null || value.length() > MAX_REGISTERED_VALUE_LENGTH) {
            invalidValue();
        }
        switch (tipoInput) {
            case NUMERICO -> validateNumeric(value);
            case TEXTO_CURTO, TEXTO_LONGO -> {
                if (value.isBlank()) {
                    invalidValue();
                }
            }
            case SELECAO_UNICA -> {
                if (!UUID_VALUE.matcher(value).matches()) {
                    invalidValue();
                }
                UUID.fromString(value);
            }
            case SELECAO_MULTIPLA -> throw new AtividadeFormularioValidacaoException(
                    "ACTIVITY_FORM_INPUT_TYPE_UNSUPPORTED", "multiple selection is not supported for activity registration");
        }
    }

    private void validateNumeric(String value) {
        if (!NUMERIC_VALUE.matcher(value).matches()) {
            invalidValue();
        }
        try {
            if (!Double.isFinite(Double.parseDouble(value))) {
                invalidValue();
            }
        } catch (NumberFormatException exception) {
            invalidValue();
        }
    }

    private void invalidValue() {
        throw new AtividadeFormularioValidacaoException(
                "ACTIVITY_FORM_INVALID_VALUE", "submitted activity form value is invalid");
    }

    private void validateProgressionCompatibility(AtividadeFormulario formulario,
                                                  Map<UUID, FatorCalculo> fatores,
                                                  com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration resolved) {
        for (CampoFormularioJson campo : formulario.getFormularioJson().campos()) {
            if (!campo.tipoInput().ehValorNumerico()) {
                continue;
            }
            FatorCalculo fator = fatores.get(campo.fatorCalculoId());
            String key = resolved.factKeyGeneration() == FactKeyGeneration.LEGACY_UUID
                    ? fator.getId().getValue().toString()
                    : fator.getSemanticKey();
            if (key == null || !resolved.numericFactorKeys().contains(key)) {
                throw new AtividadeFormularioConfiguracaoException(
                        "ACTIVITY_FORM_PROGRESSION_CONFIGURATION_MISMATCH",
                        "numeric activity form field is not represented by the active progression configuration");
            }
        }
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
